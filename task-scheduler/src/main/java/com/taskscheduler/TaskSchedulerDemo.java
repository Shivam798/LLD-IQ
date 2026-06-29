package com.taskscheduler;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.TaskScheduler;
import com.taskscheduler.strategy.CronExpression;
import com.taskscheduler.strategy.FixedDelaySchedule;
import com.taskscheduler.strategy.FixedRateSchedule;
import com.taskscheduler.strategy.OneTimeSchedule;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Runnable walkthrough of the cron-like Task Scheduler:
 *   1. a one-time task
 *   2. a fixed-RATE task (start-to-start cadence)
 *   3. a fixed-DELAY task (end-to-start cadence) whose job is slow
 *   4. a cron expression, whose next fire times we print (cron fires on minute
 *      boundaries, too slow to watch live in a few-second demo)
 *   5. cancelling a recurring task
 *   6. clean shutdown
 */
public class TaskSchedulerDemo {

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public static void main(String[] args) throws InterruptedException {
        TaskScheduler scheduler = new TaskScheduler(4);
        scheduler.start();

        Instant now = Instant.now();

        System.out.println("=== Scheduling tasks ===");

        // 1. One-time, ~1s from now.
        scheduler.schedule("one-time-greeting",
                () -> log("Hello from the one-time task"),
                new OneTimeSchedule(now.plusSeconds(1)));

        // 2. Fixed-rate every 1s (start-to-start), first run ~1s from now.
        Task heartbeat = scheduler.schedule("heartbeat",
                () -> log("heartbeat tick (fixed-rate 1s)"),
                new FixedRateSchedule(now.plusSeconds(1), Duration.ofSeconds(1)));

        // 3. Fixed-delay 1s after completion; the job itself takes ~600ms, so the
        //    gap between runs is ~1.6s — proving end-to-start spacing.
        scheduler.schedule("slow-cleanup",
                () -> {
                    log("cleanup START (runs ~600ms)");
                    sleep(600);
                    log("cleanup END");
                },
                new FixedDelaySchedule(now.plusSeconds(1), Duration.ofSeconds(1)));

        // 4. Cron: every 5 minutes, 09:00-17:59, Monday-Friday. Show next 3 fires.
        CronExpression cron = CronExpression.parse("*/5 9-17 * * 1-5");
        System.out.println("\n=== Cron '" + cron + "' next 3 fire times ===");
        Instant cursor = Instant.now();
        for (int i = 1; i <= 3; i++) {
            cursor = cron.nextAfter(cursor);
            System.out.println("   #" + i + " -> " + CLOCK.format(cursor)
                    + " (" + cursor.atZone(ZoneId.systemDefault()).getDayOfWeek() + ")");
        }

        // Let the live tasks run for ~4s.
        System.out.println("\n=== Live execution (watch fixed-rate vs fixed-delay) ===");
        Thread.sleep(4_200);

        // 5. Cancel the heartbeat; it should stop ticking.
        scheduler.cancel(heartbeat.getId());
        log("cancelled heartbeat (" + heartbeat.getId() + ")");

        Thread.sleep(1_500);

        // 6. Shut down.
        scheduler.shutdown();
        log("scheduler shut down — bye");
    }

    private static void log(String message) {
        System.out.println("  [" + CLOCK.format(Instant.now()) + "] ["
                + Thread.currentThread().getName() + "] " + message);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
