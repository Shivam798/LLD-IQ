package com.splitwise.model;

import java.util.UUID;

/**
 * A person who can pay for expenses, participate in splits, and belong to groups.
 * Identity is a UUID rather than (name, email) so users can share names or change
 * emails without breaking references held in balance sheets and expenses.
 *
 * Each user owns their own {@link BalanceSheet} — the ledger is intentionally
 * scoped to the user (SRP): User holds identity, BalanceSheet holds money relationships.
 */
public class User {
    private final String id;
    private final String name;
    private final String email;
    // Composition: BalanceSheet's lifetime is tied to its User. Killing the user
    // implicitly kills their ledger — no orphan sheets to clean up.
    private final BalanceSheet balanceSheet;

    public User(String name, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.balanceSheet = new BalanceSheet(this);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public BalanceSheet getBalanceSheet() {
        return balanceSheet;
    }
}
