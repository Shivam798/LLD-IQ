package com.taskscheduler.strategy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Fire every {@code period}, measured <b>start-to-start</b>. The next fire time is
 * the previous <i>scheduled</i> slot plus the period — independent of how long the
 * job actually ran. So a 1s-rate task fires at T, T+1, T+2, ... even if individual
 * runs take 300ms.
 *
 * <p><b>Drift / pile-up note:</b> if a run takes longer than the period, the next
 * slot is already in the past, so the engine fires it immediately and the cadence
 * effectively "catches up". This mirrors {@code ScheduledThreadPoolExecutor}'s
 * fixed-rate behaviour — fixed rate cares about <i>slots</i>, not gaps.
 */
public final class FixedRateSchedule implements Schedule {

    private final Instant firstRun;
    private final Duration period;

    public FixedRateSchedule(Instant firstRun, Duration period) {
        if (firstRun == null || period == null) {
            throw new IllegalArgumentException("firstRun and period are required");
        }
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("period must be positive");
        }
        this.firstRun = firstRun;
        this.period = period;
    }

    @Override
    public Optional<Instant> nextExecutionTime(ExecutionContext context) {
        if (context.getRunCount() == 0) {
            return Optional.of(firstRun);
        }
        // start-to-start: anchor on the slot the last run was MEANT to start.
        return Optional.of(context.getLastScheduledTime().plus(period));
    }

    @Override
    public String toString() {
        return "FixedRate(every " + period.toMillis() + "ms from " + firstRun + ")";
    }
}
