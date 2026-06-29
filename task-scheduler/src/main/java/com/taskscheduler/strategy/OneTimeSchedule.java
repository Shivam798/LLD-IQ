package com.taskscheduler.strategy;

import java.time.Instant;
import java.util.Optional;

/**
 * Fire exactly once, at a fixed instant. The simplest schedule and the base case
 * for "recurring is just one-time that keeps producing a next time".
 */
public final class OneTimeSchedule implements Schedule {

    private final Instant runAt;

    public OneTimeSchedule(Instant runAt) {
        if (runAt == null) {
            throw new IllegalArgumentException("runAt is required");
        }
        this.runAt = runAt;
    }

    @Override
    public Optional<Instant> nextExecutionTime(ExecutionContext context) {
        // Hand out the fire time only before the first run; afterwards there is none.
        return context.getRunCount() == 0 ? Optional.of(runAt) : Optional.empty();
    }

    @Override
    public String toString() {
        return "OneTime@" + runAt;
    }
}
