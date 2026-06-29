package com.taskscheduler.strategy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Fire every {@code delay}, measured <b>end-to-start</b>. The next fire time is the
 * previous run's <i>completion</i> plus the delay, so there is always at least
 * {@code delay} of quiet time between runs no matter how long each run takes.
 *
 * <p>Contrast with {@link FixedRateSchedule}: fixed-delay can never pile up, because
 * the clock for the next run does not even start until the current run finishes.
 * Use this for jobs where overlapping/catching-up would be harmful (e.g. polling a
 * resource that must not be hammered).
 */
public final class FixedDelaySchedule implements Schedule {

    private final Instant firstRun;
    private final Duration delay;

    public FixedDelaySchedule(Instant firstRun, Duration delay) {
        if (firstRun == null || delay == null) {
            throw new IllegalArgumentException("firstRun and delay are required");
        }
        if (delay.isZero() || delay.isNegative()) {
            throw new IllegalArgumentException("delay must be positive");
        }
        this.firstRun = firstRun;
        this.delay = delay;
    }

    @Override
    public Optional<Instant> nextExecutionTime(ExecutionContext context) {
        if (context.getRunCount() == 0) {
            return Optional.of(firstRun);
        }
        // end-to-start: anchor on when the last run actually FINISHED.
        return Optional.of(context.getLastCompletionTime().plus(delay));
    }

    @Override
    public String toString() {
        return "FixedDelay(" + delay.toMillis() + "ms after completion, from " + firstRun + ")";
    }
}
