package com.splitwise.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Net-balance ledger owned by a single {@link User}. Tracks "how much the owner owes
 * to (or is owed by) every other user they have ever transacted with" — a sparse map
 * keyed by counterparty.
 *
 * Sign convention:
 *   balances.get(other) > 0  → other owes owner money (owner is the creditor)
 *   balances.get(other) < 0  → owner owes other money (owner is the debtor)
 *
 * Why per-user rather than a single global ledger: each user has a natural
 * "What do I owe / who owes me" view that's cheap to render. The two-sided update
 * in SplitwiseService keeps both ledgers mirror-consistent.
 */
public class BalanceSheet {
    // The user this sheet belongs to — used in display + to short-circuit self-debt.
    private final User owner;

    // ConcurrentHashMap because multiple concurrent expense creations / settlements
    // can touch the same balance sheet from different threads. Even though
    // adjustBalance() is synchronized, reads (showBalances, simplifyGroupDebts) can
    // run lock-free and still see a consistent snapshot per key.
    private final Map<User, Double> balances = new ConcurrentHashMap<>();

    public BalanceSheet(User owner) {
        this.owner = owner;
    }

    public Map<User, Double> getBalances() {
        return balances;
    }

    /**
     * Apply a signed delta to the balance owed by {@code otherUser}. Positive
     * delta means otherUser now owes owner more; negative means owner owes more.
     *
     * Synchronized to serialize concurrent updates to the same counterparty —
     * merge() on ConcurrentHashMap is atomic per entry, but pairing it with the
     * mirrored update in the caller (see SplitwiseService.createExpense) needs
     * an outer guarantee that the pair is applied without interleaving.
     */
    public synchronized void adjustBalance(User otherUser, double amount) {
        // No one owes themselves — guards against a stray "paidBy == participant" case
        // (which createExpense already filters, but this is a defensive belt-and-braces).
        if (owner.equals(otherUser)) {
            return;
        }
        balances.merge(otherUser, amount, Double::sum);
    }

    /**
     * Pretty-print the balance sheet. Filters anything within $0.01 of zero to
     * absorb floating-point noise from percentage-based splits — without this
     * tolerance we'd print spurious "owes $0.00" lines.
     */
    public void showBalances() {
        System.out.println("--- Balance Sheet for " + owner.getName() + " ---");
        if (balances.isEmpty()) {
            System.out.println("All settled up!");
            System.out.println("---------------------------------");
            return;
        }

        double totalOwedToMe = 0;
        double totalIOwe = 0;

        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            User otherUser = entry.getKey();
            double amount = entry.getValue();

            if (amount > 0.01) {
                System.out.println(otherUser.getName() + " owes " + owner.getName()
                        + " $" + String.format("%.2f", amount));
                totalOwedToMe += amount;
            } else if (amount < -0.01) {
                System.out.println(owner.getName() + " owes " + otherUser.getName()
                        + " $" + String.format("%.2f", -amount));
                totalIOwe += (-amount);
            }
        }
        System.out.println("Total Owed to " + owner.getName() + ": $" + String.format("%.2f", totalOwedToMe));
        System.out.println("Total " + owner.getName() + " Owes: $" + String.format("%.2f", totalIOwe));
        System.out.println("---------------------------------");
    }
}
