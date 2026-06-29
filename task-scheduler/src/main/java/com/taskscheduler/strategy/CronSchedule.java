package com.taskscheduler.strategy;

import java.time.Instant;
import java.util.Optional;

/**
 * Adapts a {@link CronExpression} to the {@link Schedule} contract. Cron jobs recur
 * forever, so this never returns empty — it always hands back the next matching
 * instant after the previous completion (or after {@code now} for the first run).
 *
 * <p>Anchoring on completion (not the scheduled slot) means a slow run can cause the
 * scheduler to skip an intermediate tick rather than queue up a backlog — the right
 * default for wall-clock cron, where "run at 9:00, 9:05, 9:10" should not stack if
 * the 9:00 run overran to 9:07.
 */
public final class CronSchedule implements Schedule {

    private final CronExpression expression;

    public CronSchedule(String cronExpression) {
        this.expression = CronExpression.parse(cronExpression);
    }

    public CronSchedule(CronExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("expression is required");
        }
        this.expression = expression;
    }

    @Override
    public Optional<Instant> nextExecutionTime(ExecutionContext context) {
        Instant base = context.getLastCompletionTime() != null
                ? context.getLastCompletionTime()
                : context.getNow();
        return Optional.of(expression.nextAfter(base));
    }

    @Override
    public String toString() {
        return "Cron(" + expression + ")";
    }
}
