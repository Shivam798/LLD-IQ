package com.splitwise.strategy;

import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Divides the total equally across all participants. Simplest of the three
 * strategies — splitValues is ignored entirely.
 *
 * Note on rounding: $10 split across 3 people gives $3.333... per head. We
 * accept the floating-point dust since BalanceSheet's display layer absorbs
 * anything under $0.01. In production you'd round to cents and distribute
 * the residue cent to one participant (often the payer).
 */
public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public List<Split> calculateSplits(double totalAmount, User paidBy,
                                       List<User> participants, List<Double> splitValues) {
        List<Split> splits = new ArrayList<>();
        double amountPerPerson = totalAmount / participants.size();
        for (User participant : participants) {
            splits.add(new Split(participant, amountPerPerson));
        }
        return splits;
    }
}
