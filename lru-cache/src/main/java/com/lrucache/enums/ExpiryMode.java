package com.lrucache.enums;

/**
 * When an entry's deadline is stamped -- the single knob that decides whether
 * reading an entry buys it more life.
 *
 * Guava and Caffeine expose exactly these two as `expireAfterWrite` and
 * `expireAfterAccess`, and they are genuinely different policies rather than
 * two spellings of one idea:
 *
 *   AFTER_WRITE  : the deadline is fixed at insert. An entry dies on schedule
 *                  no matter how popular it is. This is what you want for
 *                  *invalidation* -- "this data is stale after 5 minutes,
 *                  full stop" -- because a key that is polled constantly is
 *                  exactly the key most likely to be serving stale data.
 *
 *   AFTER_ACCESS : every read pushes the deadline out. An entry dies only
 *                  after going untouched for the whole TTL. This is what you
 *                  want for *idle reclamation* -- "drop sessions nobody has
 *                  used in 30 minutes" -- where the goal is releasing memory,
 *                  not correctness.
 *
 * The trap: AFTER_ACCESS means a key that is polled forever NEVER expires.
 * If you reached for TTL to guarantee freshness, that silently defeats it.
 * AFTER_WRITE is the safer default and is what this cache uses unless told
 * otherwise.
 */
public enum ExpiryMode {

    /** Deadline set once, at write time. Reads do not extend it. */
    AFTER_WRITE,

    /** Deadline pushed forward on every read. Only idle entries expire. */
    AFTER_ACCESS
}
