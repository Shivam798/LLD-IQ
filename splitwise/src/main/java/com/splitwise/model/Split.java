package com.splitwise.model;

/**
 * A single line item inside an {@link Expense} — "user X owes amount Y for this expense".
 * Deliberately immutable: once a {@link SplitStrategy} computes the splits for an expense,
 * those numbers are frozen. Any later change (settlement, edit) creates a new expense /
 * transaction rather than mutating an existing Split.
 */
public class Split {
    private final User user;
    private final double amount;

    public Split(User user, double amount) {
        this.user = user;
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }
}
