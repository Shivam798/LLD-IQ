package com.taskscheduler.model;

import com.taskscheduler.enums.TaskStatus;
import com.taskscheduler.strategy.Schedule;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One schedulable unit: a piece of work ({@link Runnable}) plus the {@link Schedule}
 * that decides when it runs. The task carries its own next-fire time so it can live
 * directly in the scheduler's priority queue ordered by that time.
 *
 * <p><b>What's immutable vs mutable.</b> Identity, the job, and the schedule are
 * {@code final} — they never change. The scheduling bookkeeping ({@code status},
 * {@code nextExecutionTime}, run counters) <i>does</i> change over a recurring task's
 * life, so those fields are {@code volatile}: they are written by a worker thread and
 * read by the dispatcher thread, and {@code volatile} gives the cross-thread
 * visibility without a lock. They are only ever <i>mutated</i> while the scheduler
 * holds its lock, so no read-modify-write race exists despite being plain volatiles.
 *
 * <p><b>{@code sequenceNumber}</b> is a monotonic insertion id used only as a
 * tie-breaker in the priority queue, so two tasks due at the exact same instant fire
 * in submission order (FIFO) instead of an arbitrary one — deterministic and fair.
 */
public class Task {

    // Process-wide id source — AtomicLong so concurrent submissions never collide.
    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    private final String id;
    private final String name;
    private final Runnable job;
    private final Schedule schedule;
    private final long sequenceNumber;

    private volatile TaskStatus status;
    private volatile Instant nextExecutionTime;
    private volatile Instant lastScheduledTime;
    private volatile Instant lastCompletionTime;
    private volatile long runCount;

    public Task(String name, Runnable job, Schedule schedule) {
        if (job == null || schedule == null) {
            throw new IllegalArgumentException("job and schedule are required");
        }
        long seq = SEQUENCE.getAndIncrement();
        this.sequenceNumber = seq;
        this.id = "task-" + seq;
        this.name = (name == null || name.isBlank()) ? this.id : name;
        this.job = job;
        this.schedule = schedule;
        this.status = TaskStatus.SCHEDULED;
        this.runCount = 0;
    }

    /**
     * Record that a run just finished. Bumps the run counter and remembers the slot
     * the run was scheduled for and when it actually completed — these feed the next
     * {@code Schedule} computation. Called by the scheduler under its lock.
     */
    public void recordExecution(Instant scheduledTime, Instant completionTime) {
        this.lastScheduledTime = scheduledTime;
        this.lastCompletionTime = completionTime;
        this.runCount++;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Runnable getJob() {
        return job;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Instant getNextExecutionTime() {
        return nextExecutionTime;
    }

    public void setNextExecutionTime(Instant nextExecutionTime) {
        this.nextExecutionTime = nextExecutionTime;
    }

    public Instant getLastScheduledTime() {
        return lastScheduledTime;
    }

    public Instant getLastCompletionTime() {
        return lastCompletionTime;
    }

    public long getRunCount() {
        return runCount;
    }

    @Override
    public String toString() {
        return name + "[" + status + ", runs=" + runCount + ", next=" + nextExecutionTime + "]";
    }
}
