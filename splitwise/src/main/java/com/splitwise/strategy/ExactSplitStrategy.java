package com.splitwise.strategy;

import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Each participant pays a caller-specified exact amount. The caller has all the
 * freedom (and all the responsibility) — we just validate two invariants:
 *
 *  1. participants.size() == splitValues.size() (each participant has one amount)
 *  2. sum(splitValues) == totalAmount (within $0.01 tolerance to absorb FP noise)
 *
 * Validation happens here, NOT in SplitwiseService — keeps each strategy
 * self-policing (SRP) and means SplitwiseService doesn't grow a switch over
 * split types.
 */
public class ExactSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> calculateSplits(double totalAmount, User paidBy,
                                       List<User> participants, List<Double> splitValues) {
        if (splitValues == null || participants.size() != splitValues.size()) {
            throw new IllegalArgumentException("Number of participants and split values must match.");
        }
        // Float-tolerant equality check — direct == on doubles would reject legitimate
        // splits like (33.33 + 33.33 + 33.34 == 100.00).
        double sum = splitValues.stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - totalAmount) > 0.01) {
            throw new IllegalArgumentException("Sum of exact amounts must equal the total expense amount.");
        }

        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            splits.add(new Split(participants.get(i), splitValues.get(i)));
        }
        return splits;
    }
}
