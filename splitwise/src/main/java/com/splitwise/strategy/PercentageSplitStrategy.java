package com.splitwise.strategy;

import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an expense by caller-specified percentages.
 *
 * Worked example:
 *   totalAmount   = 500
 *   participants  = [Alice, Bob, Charlie]
 *   splitValues   = [40.0, 30.0, 30.0]    // % each owes
 *                       ↓
 *   produces      = [Split(Alice, 200), Split(Bob, 150), Split(Charlie, 150)]
 *                       (500 × 40/100, 500 × 30/100, 500 × 30/100)
 *
 * Two invariants this strategy enforces on its inputs:
 *   1. participants.size() == splitValues.size()
 *        — every participant must have exactly one percentage assigned.
 *   2. sum(splitValues) ≈ 100  (with $0.01 tolerance)
 *        — the percentages must form a complete pie. Strict == fails for
 *          legitimate splits like 33.33 + 33.33 + 33.34 = 100.00.
 *
 * Why percentages instead of just letting the caller pass exact amounts
 * (which is what ExactSplitStrategy does)?
 *   - Percentages express *intent* that survives changes to the total.
 *     If you said "Alice covers 60%" and later realize tax was missed and
 *     the total jumps from $100 → $110, percentages still describe the
 *     deal correctly ($66 instead of $60). Exact amounts would lie.
 *   - Real-world Splitwise use: rent splits, utility shares — the
 *     proportion is the agreement, the dollar amount is downstream.
 *
 * SRP: this strategy validates its own inputs and computes its own math.
 * It does NOT touch any BalanceSheet — that's SplitwiseService's job once
 * the List<Split> is returned.
 */
public class PercentageSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> calculateSplits(double totalAmount, User paidBy,
                                       List<User> participants, List<Double> splitValues) {

        // ─────────────────────────────────────────────────────────────
        // Guard #1: every participant must have a matching percentage.
        //
        // Both null-check and size-check happen here because either
        // mistake at the call site (forgot to call setSplitValues, or
        // passed a list one element short) would otherwise NPE / produce
        // silently wrong splits deep inside the loop. Fail fast with a
        // message that tells the caller exactly what's wrong.
        // ─────────────────────────────────────────────────────────────
        if (splitValues == null || participants.size() != splitValues.size()) {
            throw new IllegalArgumentException(
                    "Number of participants and split values must match.");
        }

        // ─────────────────────────────────────────────────────────────
        // Guard #2: percentages must add up to 100.
        //
        // We use a $0.01 epsilon comparison instead of `== 100.0` because
        // doubles can't represent 1/3 exactly:
        //
        //     33.33 + 33.33 + 33.34   →   100.00000000000001  (in IEEE 754)
        //
        // Strict equality would reject that perfectly valid split. The
        // 0.01 tolerance mirrors the cent-precision of money — any error
        // smaller than a cent is below the resolution we care about.
        // ─────────────────────────────────────────────────────────────
        double sum = splitValues.stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 100.0) > 0.01) {
            throw new IllegalArgumentException("Sum of percentages must be 100.");
        }

        // ─────────────────────────────────────────────────────────────
        // Compute the splits.
        //
        // For each participant i:
        //     amount_i = totalAmount × splitValues[i] / 100
        //
        // The order of Split objects in the returned list matches the
        // order of `participants` — the caller relies on this when
        // associating users with their owed amounts (see how Expense.build
        // hands these straight back without re-keying).
        //
        // We deliberately do NOT round to cents here. The leftover
        // floating-point dust (fractions of a cent) is absorbed at the
        // display layer in BalanceSheet.showBalances(), which filters
        // anything with |balance| < $0.01. Rounding here would force us
        // to also distribute the residue cent somewhere — a complication
        // that real Splitwise handles but isn't worth the code in an
        // interview-grade design.
        // ─────────────────────────────────────────────────────────────
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            double amount = (totalAmount * splitValues.get(i)) / 100.0;
            splits.add(new Split(participants.get(i), amount));
        }
        return splits;
    }
}
