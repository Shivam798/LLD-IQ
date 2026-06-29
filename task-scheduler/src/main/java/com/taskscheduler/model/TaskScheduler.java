package com.taskscheduler.model;

import com.taskscheduler.enums.TaskStatus;
import com.taskscheduler.strategy.ExecutionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The engine. It owns a time-ordered priority queue of tasks, a single dispatcher
 * thread that decides <i>when</i> to fire, and a worker pool that does the actual
 * work. This is the same architecture as the JDK's {@code ScheduledThreadPoolExecutor},
 * built explicitly to show the moving parts.
 *
 * <h3>Why a min-heap keyed by fire time</h3>
 * Only the earliest task ever matters next, so a {@link PriorityQueue} (binary
 * min-heap) gives O(1) "peek the soonest" and O(log n) insert/remove. A sorted list
 * would be O(n) to insert; an unsorted list O(n) to find the minimum every tick.
 *
 * <h3>Why a lock + condition instead of {@code Thread.sleep}</h3>
 * The dispatcher must sleep until the head task is due — but if a <i>new, earlier</i>
 * task arrives while it sleeps, it has to wake up and re-evaluate. A plain
 * {@code Thread.sleep(delay)} can't be interrupted by "something better arrived"
 * cleanly. So the dispatcher does a <b>timed</b> {@code condition.await(delay)}, and
 * any mutation of the queue ({@code schedule}/{@code cancel}/reschedule) calls
 * {@code signalAll()} to wake it so it recomputes the soonest deadline. This is the
 * classic leader-wait / DelayQueue pattern.
 *
 * <h3>Why a separate worker pool</h3>
 * The dispatcher must never run a job itself — a single slow job would stall every
 * future tick. It only <i>hands off</i> due tasks to an {@link ExecutorService}; the
 * pool runs them concurrently. Deciding-when is decoupled from doing-the-work.
 *
 * <h3>The priority-queue invariant</h3>
 * A heap must never have a key mutated while the element sits inside it. We honour
 * that: {@code nextExecutionTime} is only ever changed while the task is OUT of the
 * queue (before the first {@code add}, or after a {@code poll} and before re-adding
 * in {@code reschedule}). Every queue mutation happens under {@link #lock}.
 *
 * <h3>Not a singleton</h3>
 * Deliberately instantiable — you may want several schedulers with different pool
 * sizes, and a constructor-injected pool is far more testable than a global instance.
 */
public class TaskScheduler {

    private final PriorityQueue<Task> queue;
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final ExecutorService workerPool;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();

    private volatile boolean running = false;
    private Thread dispatcherThread;

    public TaskScheduler(int workerThreads) {
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be positive");
        }
        this.queue = new PriorityQueue<>(
                Comparator.comparing(Task::getNextExecutionTime)
                        .thenComparingLong(Task::getSequenceNumber));
        AtomicLong workerId = new AtomicLong(1);
        this.workerPool = Executors.newFixedThreadPool(workerThreads, r -> {
            Thread t = new Thread(r, "task-worker-" + workerId.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    /** Start the dispatcher thread. Idempotent. */
    public void start() {
        lock.lock();
        try {
            if (running) {
                return;
            }
            running = true;
            dispatcherThread = new Thread(this::runDispatcher, "task-scheduler-dispatcher");
            dispatcherThread.setDaemon(true);
            dispatcherThread.start();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Submit a job with a schedule. Computes the first fire time eagerly; if the
     * schedule yields none (e.g. an already-exhausted one-time), the task is marked
     * COMPLETED and never enqueued.
     */
    public Task schedule(String name, Runnable job, com.taskscheduler.strategy.Schedule schedule) {
        Task task = new Task(name, job, schedule);
        lock.lock();
        try {
            ExecutionContext ctx = new ExecutionContext(0, null, null, Instant.now());
            Optional<Instant> first = schedule.nextExecutionTime(ctx);
            if (first.isEmpty()) {
                task.setStatus(TaskStatus.COMPLETED);
                return task;
            }
            task.setNextExecutionTime(first.get());
            task.setStatus(TaskStatus.SCHEDULED);
            tasks.put(task.getId(), task);
            queue.add(task);
            available.signalAll(); // the new task may be sooner than the current head
        } finally {
            lock.unlock();
        }
        return task;
    }

    /**
     * Cancel a task. If it is still in the queue it is removed; if it has already
     * been dequeued for running, the CANCELLED status makes the dispatcher/worker
     * skip it. Returns false if the id is unknown.
     */
    public boolean cancel(String taskId) {
        lock.lock();
        try {
            Task task = tasks.remove(taskId);
            if (task == null) {
                return false;
            }
            task.setStatus(TaskStatus.CANCELLED);
            queue.remove(task);
            available.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Stop accepting/firing work and shut the worker pool down. */
    public void shutdown() {
        lock.lock();
        try {
            running = false;
            available.signalAll();
        } finally {
            lock.unlock();
        }
        workerPool.shutdown();
    }

    // ---- internals -------------------------------------------------------

    private void runDispatcher() {
        while (running) {
            try {
                lock.lockInterruptibly();
                try {
                    while (running && queue.isEmpty()) {
                        available.await(); // nothing to do — sleep until a task arrives
                    }
                    if (!running) {
                        break;
                    }
                    Task head = queue.peek();
                    Instant now = Instant.now();
                    if (!head.getNextExecutionTime().isAfter(now)) {
                        // Due now — pop it and hand off.
                        queue.poll();
                        if (head.getStatus() == TaskStatus.CANCELLED) {
                            continue;
                        }
                        dispatch(head);
                    } else {
                        // Not due yet — wait until its deadline, but wake early if
                        // signalAll() fires because an earlier task was added.
                        long waitMs = Duration.between(now, head.getNextExecutionTime()).toMillis();
                        available.await(Math.max(waitMs, 1), TimeUnit.MILLISECONDS);
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Caller holds {@link #lock}. Marks RUNNING and hands the task to the pool. */
    private void dispatch(Task task) {
        Instant scheduledTime = task.getNextExecutionTime();
        task.setStatus(TaskStatus.RUNNING);
        workerPool.submit(() -> runTask(task, scheduledTime));
    }

    /** Runs on a worker thread: execute the job, then ask the schedule what's next. */
    private void runTask(Task task, Instant scheduledTime) {
        boolean failed = false;
        try {
            task.getJob().run();
        } catch (Throwable t) {
            failed = true;
            System.err.println("[scheduler] task " + task.getId() + " threw: " + t);
        }
        reschedule(task, scheduledTime, Instant.now(), failed);
    }

    private void reschedule(Task task, Instant scheduledTime, Instant completion, boolean failed) {
        lock.lock();
        try {
            if (task.getStatus() == TaskStatus.CANCELLED) {
                return; // cancelled mid-run: do not requeue
            }
            task.recordExecution(scheduledTime, completion);
            ExecutionContext ctx = new ExecutionContext(
                    task.getRunCount(), scheduledTime, completion, Instant.now());
            Optional<Instant> next = task.getSchedule().nextExecutionTime(ctx);
            if (next.isPresent()) {
                task.setNextExecutionTime(next.get());
                task.setStatus(TaskStatus.SCHEDULED);
                queue.add(task);
                available.signalAll();
            } else {
                task.setStatus(failed ? TaskStatus.FAILED : TaskStatus.COMPLETED);
                tasks.remove(task.getId());
            }
        } finally {
            lock.unlock();
        }
    }
}
