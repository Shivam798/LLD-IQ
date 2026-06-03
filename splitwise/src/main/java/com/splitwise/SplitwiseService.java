package com.splitwise;

import com.splitwise.model.Expense;
import com.splitwise.model.Group;
import com.splitwise.model.Split;
import com.splitwise.model.Transaction;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Singleton facade that owns the user / group catalog and routes every operation —
 * creating an expense, settling up, showing balances, simplifying group debts.
 *
 * Why a single service object and not a bag of free functions:
 *   - Centralizes thread-safety policy: every mutating call goes through one
 *     synchronized boundary, so callers can't accidentally race.
 *   - Gives one obvious entry point for higher layers (CLI, REST, demo) — the
 *     entities themselves stay pure data + ledger.
 *   - Implements the Facade pattern: "expense flow" hides the dance of
 *     building Splits, mirroring updates across two BalanceSheets, etc.
 */
public class SplitwiseService {
    // 'volatile' is the key to double-checked locking — without it, another thread
    // could observe a non-null `instance` whose fields are not yet visible to it.
    private static volatile SplitwiseService instance;

    // ConcurrentHashMap because admin ops (addUser, addGroup) and user ops
    // (createExpense, settleUp, showBalanceSheet) can interleave from many threads.
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    private SplitwiseService() {
    }

    /**
     * Double-checked locking Singleton accessor. Outer null-check skips the
     * synchronized cost on the hot path; inner null-check handles the race
     * where two threads both pass the outer check before either constructs.
     */
    public static SplitwiseService getInstance() {
        if (instance == null) {
            synchronized (SplitwiseService.class) {
                if (instance == null) {
                    instance = new SplitwiseService();
                }
            }
        }
        return instance;
    }

    // ---------- Catalog (setup) ----------

    public User addUser(String name, String email) {
        User user = new User(name, email);
        users.put(user.getId(), user);
        return user;
    }

    public Group addGroup(String name, List<User> members) {
        Group group = new Group(name, members);
        groups.put(group.getId(), group);
        return group;
    }

    public User getUser(String id) {
        return users.get(id);
    }

    public Group getGroup(String id) {
        return groups.get(id);
    }

    // ---------- Core operations ----------

    /**
     * Records a new expense and mirrors the effect on both sides of every
     * participant ↔ payer relationship. Synchronized at the service level so
     * a concurrent createExpense + settleUp on the same pair of users can't
     * leave the two BalanceSheets out of sync.
     *
     * Two-sided update invariant: for every (paidBy, participant) pair, we apply
     *   paidBy.sheet[participant]      +=  amount   (participant owes paidBy more)
     *   participant.sheet[paidBy]      += -amount   (paidBy owes participant less / negative is participant's debt)
     * Without both updates, the two ledgers diverge and showBalances would lie
     * depending on which user you look at.
     */
    public synchronized void createExpense(Expense.ExpenseBuilder builder) {
        Expense expense = builder.build();
        User paidBy = expense.getPaidBy();

        for (Split split : expense.getSplits()) {
            User participant = split.getUser();
            double amount = split.getAmount();

            // Skip the "paidBy is also a participant" line — they don't owe
            // themselves anything, and adjustBalance would no-op anyway. Skipping
            // here avoids two pointless map operations per expense.
            if (!paidBy.equals(participant)) {
                paidBy.getBalanceSheet().adjustBalance(participant, amount);
                participant.getBalanceSheet().adjustBalance(paidBy, -amount);
            }
        }
        System.out.println("Expense '" + expense.getDescription() + "' of amount "
                + expense.getAmount() + " created.");
    }

    /**
     * Records a settlement: payer hands cash to payee, debt shrinks on both sides.
     * Mechanically it's the inverse of an expense — the payer's debt to the payee
     * decreases by {@code amount}. Synchronized for the same two-sided-update
     * reason as createExpense.
     */
    public synchronized void settleUp(String payerId, String payeeId, double amount) {
        User payer = users.get(payerId);
        User payee = users.get(payeeId);
        if (payer == null || payee == null) {
            throw new IllegalArgumentException("Unknown payer or payee");
        }
        System.out.println(payer.getName() + " is settling up " + amount + " with " + payee.getName());

        // Mirror the sign convention from createExpense, but inverted:
        //   payee's sheet vs payer:  payer owes less → subtract from positive
        //   payer's sheet vs payee:  payer's debt shrinks → balance moves toward 0
        payee.getBalanceSheet().adjustBalance(payer, -amount);
        payer.getBalanceSheet().adjustBalance(payee, amount);
    }

    public void showBalanceSheet(String userId) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("Unknown user: " + userId);
            return;
        }
        user.getBalanceSheet().showBalances();
    }

    /**
     * Greedy debt-simplification within a single group: instead of every member
     * paying every other, compute the minimum set of transfers that settles all
     * intra-group balances.
     *
     * Algorithm:
     *   1. For each member, sum their balances against ONLY other group members
     *      (so cross-group debts are excluded — those settle separately).
     *   2. Split members into creditors (net positive — owed money) and debtors
     *      (net negative — owe money). Sort by magnitude.
     *   3. Repeatedly match the biggest creditor with the biggest debtor; settle
     *      min(|creditor|, |debtor|); advance whichever side hit ~0.
     *
     * This is the classic "minimum cash flow" greedy. It produces at most N-1
     * transactions for N members in the group, which is optimal in transaction
     * count for most realistic balance configurations.
     */
    public List<Transaction> simplifyGroupDebts(String groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found");
        }

        // Step 1: compute each member's net intra-group balance. We deliberately
        // filter to group members only — a member's debts to people outside the
        // group are none of this group's business.
        Map<User, Double> netBalances = new HashMap<>();
        List<User> members = group.getMembers();
        for (User member : members) {
            double balance = 0;
            for (Map.Entry<User, Double> entry : member.getBalanceSheet().getBalances().entrySet()) {
                if (members.contains(entry.getKey())) {
                    balance += entry.getValue();
                }
            }
            netBalances.put(member, balance);
        }

        // Step 2: bucket into creditors (positive) and debtors (negative), sort
        // by magnitude. Sorting matters for the greedy to produce minimal txns —
        // matching the biggest first absorbs the largest chunks fastest.
        List<Map.Entry<User, Double>> creditors = netBalances.entrySet().stream()
                .filter(e -> e.getValue() > 0.01)
                .collect(Collectors.toList());
        List<Map.Entry<User, Double>> debtors = netBalances.entrySet().stream()
                .filter(e -> e.getValue() < -0.01)
                .collect(Collectors.toList());

        creditors.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        debtors.sort(Map.Entry.comparingByValue());

        // Step 3: two-pointer greedy match. Each iteration settles at least one
        // user (creditor or debtor hits zero), so the loop runs at most |creditors|+|debtors| times.
        List<Transaction> transactions = new ArrayList<>();
        int i = 0, j = 0;
        while (i < creditors.size() && j < debtors.size()) {
            Map.Entry<User, Double> creditor = creditors.get(i);
            Map.Entry<User, Double> debtor = debtors.get(j);

            double amountToSettle = Math.min(creditor.getValue(), -debtor.getValue());
            transactions.add(new Transaction(debtor.getKey(), creditor.getKey(), amountToSettle));

            // Drain both sides by the settled amount; whichever hits ~0 advances.
            // The 0.01 epsilon absorbs floating-point dust so we don't loop forever
            // on something like creditor=1e-12 > 0.
            creditor.setValue(creditor.getValue() - amountToSettle);
            debtor.setValue(debtor.getValue() + amountToSettle);

            if (Math.abs(creditor.getValue()) < 0.01) i++;
            if (Math.abs(debtor.getValue()) < 0.01) j++;
        }
        return transactions;
    }
}
