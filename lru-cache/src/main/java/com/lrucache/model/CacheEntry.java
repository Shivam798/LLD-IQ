package com.lrucache.model;

import java.time.Duration;
import java.time.Instant;

/**
 * What the cache actually stores against a key: the caller's value, the
 * lifetime it was given, and the instant at which it stops being valid.
 *
 * Why wrap the value instead of putting expiry on the eviction policy's node?
 * Because TTL and eviction answer two different questions:
 *   - EvictionPolicy answers "we are FULL -- who leaves?" (capacity pressure)
 *   - TTL answers      "is this entry still TRUE?"        (staleness)
 * A cache with room to spare must still refuse to serve a stale entry, so
 * expiry cannot live inside a policy that only speaks up when the cache is
 * full. Keeping it on the stored entry means every policy -- LRU, LFU, FIFO,
 * TTL-ordered, or one written next year -- gets TTL for free, with no changes.
 *
 * Immutable: a new entry is created on every put -- and on every renewal --
 * rather than mutating an existing one, so a TTL can never be silently
 * extended underneath a reader.
 */
public final class CacheEntry<V> {

    private final V value;

    // The lifetime this entry was granted, kept alongside the deadline it
    // produced. Storing only expiresAt would be enough for AFTER_WRITE, but
    // AFTER_ACCESS has to recompute "now + ttl" on every read, and the entry
    // is the only place that knows its own ttl (the cache default and a
    // per-entry override are indistinguishable by the time we get here).
    private final Duration ttl;

    // Absolute deadline, not a duration. Storing "expires at 10:04:31.2Z"
    // instead of "lives 5 minutes" means checking expiry is one comparison
    // against the clock -- no arithmetic on the read path, and no dependency
    // on when the check happens to run. null means "never expires".
    private final Instant expiresAt;

    /**
     * Takes `now` and derives the deadline itself rather than accepting a
     * pre-computed expiresAt, so a caller cannot hand in a ttl and a deadline
     * that disagree.
     */
    public CacheEntry(V value, Duration ttl, Instant now) {
        this.value = value;
        this.ttl = ttl;
        this.expiresAt = ttl == null ? null : now.plus(ttl);
    }

    public V value() {
        return value;
    }

    /**
     * A copy of this entry with its deadline restarted from `now`. Used only
     * under ExpiryMode.AFTER_ACCESS. Returns `this` when there is no ttl,
     * because an entry that never expires has nothing to renew -- that check
     * saves an allocation on every read of a no-TTL entry.
     */
    public CacheEntry<V> renewed(Instant now) {
        if (ttl == null) {
            return this;
        }
        return new CacheEntry<>(value, ttl, now);
    }

    /**
     * True once the deadline has passed. `now` is passed in rather than read
     * from Instant.now() so the caller controls the clock -- the Cache reads
     * the clock once per operation and every entry in that operation is judged
     * against the same instant. That keeps a bulk purge internally consistent
     * (no entry surviving because time moved mid-loop) and makes the whole
     * thing testable with a fixed Clock.
     *
     * Uses !isBefore rather than isAfter so an entry is expired exactly AT its
     * deadline, not one nanosecond later.
     */
    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public Duration ttl() {
        return ttl;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
