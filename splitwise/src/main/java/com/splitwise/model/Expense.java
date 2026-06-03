package com.splitwise.model;

import com.splitwise.strategy.SplitStrategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * An immutable record of "person X paid Y, to be split among participants Z..." —
 * the atomic unit of activity in the system. Once built, an Expense is frozen:
 * its split distribution is computed once by the chosen {@link SplitStrategy} and
 * stored as a list of {@link Split}s.
 *
 * Constructed via the inner {@link ExpenseBuilder} (Builder pattern) because:
 *   - There are 5+ fields and a positional constructor with adjacent same-typed
 *     args (description vs paidBy.name, etc.) is error-prone at call sites.
 *   - We need a single validation chokepoint (build()) — the strategy is
 *     required, splitValues are conditionally required, etc.
 *   - The split computation happens at build time, not lazily — once you have an
 *     Expense object, its line items exist and can't drift.
 */
public class Expense {
    private final String id;
    private final String description;
    private final double amount;
    private final User paidBy;
    private final List<Split> splits;
    private final LocalDateTime timestamp;

    private Expense(ExpenseBuilder builder) {
        // ID defaults to a UUID if the caller didn't supply one. Letting callers
        // override is handy for replaying expenses with stable IDs (tests, imports).
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.description = builder.description;
        this.amount = builder.amount;
        this.paidBy = builder.paidBy;
        this.timestamp = LocalDateTime.now();

        // The Strategy does the work — Expense doesn't know whether this is
        // EQUAL, EXACT, or PERCENTAGE. New split type = new SplitStrategy impl,
        // no change here (OCP).
        this.splits = builder.splitStrategy.calculateSplits(
                builder.amount, builder.paidBy, builder.participants, builder.splitValues);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Builder for {@link Expense}. Setters return {@code this} for fluent chaining;
     * build() runs invariants and triggers the split computation. Once build() is
     * called, the resulting Expense is immutable — the builder is throwaway.
     */
    public static class ExpenseBuilder {
        private String id;
        private String description;
        private double amount;
        private User paidBy;
        private List<User> participants;
        private SplitStrategy splitStrategy;
        // Strategy-specific: amounts for EXACT, percentages for PERCENTAGE,
        // ignored by EQUAL. Each strategy validates its own contract.
        private List<Double> splitValues;

        public ExpenseBuilder setId(String id) { this.id = id; return this; }
        public ExpenseBuilder setDescription(String description) { this.description = description; return this; }
        public ExpenseBuilder setAmount(double amount) { this.amount = amount; return this; }
        public ExpenseBuilder setPaidBy(User paidBy) { this.paidBy = paidBy; return this; }
        public ExpenseBuilder setParticipants(List<User> participants) { this.participants = participants; return this; }
        public ExpenseBuilder setSplitStrategy(SplitStrategy splitStrategy) { this.splitStrategy = splitStrategy; return this; }
        public ExpenseBuilder setSplitValues(List<Double> splitValues) { this.splitValues = splitValues; return this; }

        public Expense build() {
            // Strategy is the one truly mandatory dependency — fail fast if missing
            // rather than NPE deep inside Expense's constructor.
            if (splitStrategy == null) {
                throw new IllegalStateException("Split strategy is required.");
            }
            if (paidBy == null) {
                throw new IllegalStateException("paidBy is required.");
            }
            if (participants == null || participants.isEmpty()) {
                throw new IllegalStateException("At least one participant is required.");
            }
            return new Expense(this);
        }
    }
}
