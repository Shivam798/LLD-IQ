package com.splitwise.strategy;

import com.splitwise.model.Split;
import com.splitwise.model.User;

import java.util.List;

/**
 * Strategy interface for turning "total expense + participants + optional per-user
 * values" into concrete {@link Split} line items. Each concrete strategy encodes
 * one splitting rule (equal, exact, percentage).
 *
 * Designed with a single method (ISP) — no god interface with optional methods.
 * splitValues is nullable / ignored for EQUAL but required (and validated) for
 * EXACT and PERCENTAGE. Each strategy enforces its own input contract.
 */
public interface SplitStrategy {
    /**
     * @param totalAmount   the expense total
     * @param paidBy        the user who fronted the money (informational; not always used)
     * @param participants  the users among whom to split
     * @param splitValues   strategy-specific values: amounts (EXACT), percentages (PERCENTAGE), unused (EQUAL)
     * @return one Split per participant, in the same order as the participants list
     */
    List<Split> calculateSplits(double totalAmount, User paidBy,
                                List<User> participants, List<Double> splitValues);
}
