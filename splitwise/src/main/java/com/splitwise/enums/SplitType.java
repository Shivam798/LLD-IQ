package com.splitwise.enums;

/**
 * Enumerates the three supported ways an expense can be divided among
 * participants. Kept as an enum rather than String constants so callers
 * get compile-time safety and exhaustive switch checking.
 */
public enum SplitType {
    /** Total amount divided equally across all participants. */
    EQUAL,

    /** Each participant pays a fixed amount; the sum must equal the total. */
    EXACT,

    /** Each participant pays a fixed percentage; the percentages must sum to 100. */
    PERCENTAGE
}
