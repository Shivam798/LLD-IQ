package com.taskscheduler.strategy;

import java.time.Instant;
import java.util.Optional;

/**
 * STRATEGY pattern — the policy that decides <i>when</i> a task should next run.
 *
 * <p>This is the one extension point of the whole scheduler: the engine never asks
 * "is this one-time or recurring or cron?"; it just calls {@link #nextExecutionTime}
 * and either gets an instant to enqueue or an empty result meaning "this task is
 * done, retire it". Adding a new cadence (e.g. exponential backoff) is a new class
 * here and zero changes to the engine (Open/Closed).
 *
 * <p>Returning {@link Optional} rather than a sentinel/null makes "no more runs"
 * an explicit, un-ignorable part of the contract.
 */
@FunctionalInterface
public interface Schedule {

    /**
     * @param context what happened so far (run count, last scheduled/completion times, now)
     * @return the next instant to fire, or empty if the task should not run again
     */
    Optional<Instant> nextExecutionTime(ExecutionContext context);
}
