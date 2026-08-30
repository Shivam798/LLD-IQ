package com.lrucache.model;

import java.time.Instant;

/**
 * What the cache actually stores against a key: the caller's value plus the
 * instant at which that value stops being valid.
 *
 * Why wrap the value instead of putting expiry on the eviction policy's node?
 * Because TTL and eviction answer two different questions:
 *   - EvictionPolicy answers "we are FULL -- who leaves?" (capacity pressure)
 *   - TTL answers      "is this entry still TRUE?"        (staleness)
 * A cache with room to spare must still refuse to serve a stale entry, so
 * expiry cannot live inside a policy that only speaks up when the cache is
 * full. Keeping it on the stored entry means every policy -- LRU, LFU, FIFO,
 * or one written next year -- gets TTL for free, with no changes.
 *
 * Immutable: a new entry is created on every put rather than mutating an
 * existing one, so a TTL can never be silently extended underneath a reader.
 */
public final class CacheEntry<V> {

    private final V value;

    // Absolute deadline, not a duration. Storing "expires at 10:04:31.2Z"
    // instead of "lives 5 minutes" means checking expiry is one comparison
    // against the clock -- no arithmetic, and no dependency on when the check
    // happens to run. null means "never expires".
    private final Instant expiresAt;

    public CacheEntry(V value, Instant expiresAt) {
        this.value = value;
        this.expiresAt = expiresAt;
    }

    public V value() {
        return value;
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

    public Instant expiresAt() {
        return expiresAt;
    }
}
