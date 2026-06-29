package com.taskscheduler.strategy;

import java.time.Instant;

/**
 * The information a {@link Schedule} needs to compute the next fire time, bundled
 * into one immutable value object instead of a long parameter list.
 *
 * <p>Different schedules lean on different fields, which is exactly why this is a
 * single object rather than four positional arguments:
 * <ul>
 *   <li>a one-time schedule only cares about {@code runCount} (have we run yet?);</li>
 *   <li>fixed-<b>rate</b> adds its period to {@code lastScheduledTime} (the wall-clock
 *       slot the previous run was meant to start), so ticks stay on a fixed cadence
 *       regardless of how long the job took;</li>
 *   <li>fixed-<b>delay</b> adds its delay to {@code lastCompletionTime} (when the
 *       previous run actually finished), so the gap between runs is measured end-to-start;</li>
 *   <li>cron computes the next matching instant after {@code lastCompletionTime}
 *       (or {@code now} for the very first run).</li>
 * </ul>
 *
 * <p>{@code lastScheduledTime} and {@code lastCompletionTime} are {@code null} before
 * the first run (when {@code runCount == 0}).
 */
public final class ExecutionContext {

    private final long runCount;
    private final Instant lastScheduledTime;
    private final Instant lastCompletionTime;
    private final Instant now;

    public ExecutionContext(long runCount, Instant lastScheduledTime,
                            Instant lastCompletionTime, Instant now) {
        this.runCount = runCount;
        this.lastScheduledTime = lastScheduledTime;
        this.lastCompletionTime = lastCompletionTime;
        this.now = now;
    }

    /** Number of times this task has already completed (0 before the first run). */
    public long getRunCount() {
        return runCount;
    }

    /** Wall-clock slot the previous run was scheduled to start; null before the first run. */
    public Instant getLastScheduledTime() {
        return lastScheduledTime;
    }

    /** When the previous run actually finished; null before the first run. */
    public Instant getLastCompletionTime() {
        return lastCompletionTime;
    }

    /** Reference "current" time, captured by the scheduler under its lock. */
    public Instant getNow() {
        return now;
    }
}
