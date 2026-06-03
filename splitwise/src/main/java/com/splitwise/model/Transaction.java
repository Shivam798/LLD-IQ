package com.splitwise.model;

/**
 * Records a money transfer between two users — either a real settlement
 * (X paid Y back $50) or a suggested transfer produced by the debt-simplification
 * algorithm ("X should pay Y $50").
 *
 * Immutable on purpose: once a transaction is recorded, the ledger entry shouldn't change.
 */
public class Transaction {
    private final User from;
    private final User to;
    private final double amount;

    public Transaction(User from, User to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public User getFrom() {
        return from;
    }

    public User getTo() {
        return to;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return from.getName() + " should pay " + to.getName() + " $" + String.format("%.2f", amount);
    }
}
