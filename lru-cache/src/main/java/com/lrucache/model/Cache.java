package com.lrucache.model;

import com.lrucache.enums.ExpiryMode;
import com.lrucache.strategy.EvictionPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic, fixed-capacity in-memory cache with optional time-to-live.
 *
 * Two independent reasons an entry can leave the cache, deliberately kept in
 * two different places:
 *
 *   1. CAPACITY PRESSURE -- the cache is full and a new key needs room. Who
 *      leaves is decided by the injected EvictionPolicy (LRU, LFU, FIFO...).
 *      The cache does not know or care which one it holds.
 *
 *   2. STALENESS (TTL)   -- the entry outlived its deadline and is no longer
 *      allowed to be served, whether or not the cache is full. This lives
 *      here in the Cache, on CacheEntry, and NOT in the EvictionPolicy: TTL
 *      is about truth, eviction is about space. Folding expiry into the
 *      policy interface would force every policy to reimplement it and would
 *      break SRP.
 *
 * TTL is enforced lazily on read (an expired entry is a miss, and is dropped
 * on the spot) and opportunistically on write (a full cache purges dead
 * entries before it evicts a live one). ExpiryMode chooses WHEN the deadline
 * is stamped: AFTER_WRITE (default) fixes it at insert, AFTER_ACCESS pushes
 * it forward on every read so only idle entries die. There is no background sweeper
 * thread: lazy expiry keeps the design allocation-free and thread-free, at
 * the cost of expired-but-untouched entries occupying memory until something
 * bumps into them -- which is exactly what Guava and Caffeine do. Callers
 * that care can invoke purgeExpired() themselves.
 *
 * All public methods are synchronized to keep the cache and its policy in
 * lockstep under concurrent access. Reads and writes both mutate the policy
 * (touching access order or frequency), so even get() needs the lock.
 */
public class Cache<K, V> {

    private final int capacity;
    private final Map<K, CacheEntry<V>> data;
    private final EvictionPolicy<K> policy;

    // Applied to every put that doesn't specify its own TTL. null means
    // "entries never expire", which makes TTL a strictly opt-in feature --
    // the two-arg constructor behaves exactly as it did before TTL existed.
    private final Duration defaultTtl;

    // Injected rather than calling Instant.now() directly. A Clock is the
    // standard Java seam for time: tests hand in Clock.fixed(...) and can
    // step time forward deterministically instead of calling Thread.sleep.
    // Without this seam, every TTL test is slow and flaky.
    private final Clock clock;

    // Whether a read pushes an entry's deadline forward. AFTER_WRITE is the
    // default because it is the safe one: it guarantees an entry cannot
    // outlive its TTL no matter how hot it is. AFTER_ACCESS is opt-in
    // precisely because it silently defeats that guarantee.
    private final ExpiryMode expiryMode;

    /**
     * Capacity-bounded cache with no expiry. Entries live until evicted.
     */
    public Cache(int capacity, EvictionPolicy<K> policy) {
        this(capacity, policy, null, Clock.systemUTC(), ExpiryMode.AFTER_WRITE);
    }

    /**
     * Capacity-bounded cache where every entry expires `defaultTtl` after it
     * was written. Individual puts can still override it.
     */
    public Cache(int capacity, EvictionPolicy<K> policy, Duration defaultTtl) {
        this(capacity, policy, defaultTtl, Clock.systemUTC(), ExpiryMode.AFTER_WRITE);
    }

    public Cache(int capacity, EvictionPolicy<K> policy, Duration defaultTtl, Clock clock) {
        this(capacity, policy, defaultTtl, clock, ExpiryMode.AFTER_WRITE);
    }

    public Cache(int capacity, EvictionPolicy<K> policy, Duration defaultTtl, Clock clock,
                 ExpiryMode expiryMode) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (policy == null) {
            throw new IllegalArgumentException("EvictionPolicy is required");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        if (expiryMode == null) {
            throw new IllegalArgumentException("ExpiryMode is required");
        }
        validateTtl(defaultTtl);
        this.expiryMode = expiryMode;
        this.capacity = capacity;
        this.policy = policy;
        this.defaultTtl = defaultTtl;
        this.clock = clock;
        this.data = new HashMap<>();
    }

    /**
     * Returns the value if the key is present AND still fresh.
     *
     * An expired entry is treated as a miss, and is deleted here rather than
     * merely skipped -- otherwise a key that is polled forever but never
     * re-written would leak. Note the ordering: expiry is checked BEFORE
     * policy.keyAccessed, so reading a stale entry never counts as a hit and
     * can never promote a dead key to most-recently-used.
     *
     * Under AFTER_ACCESS the surviving entry has its deadline restarted, which
     * is why this "read" writes back into `data`. The renewal happens AFTER
     * the expiry check, so a read can revive an entry that was still alive but
     * can never resurrect one that had already died.
     */
    public synchronized Optional<V> get(K key) {
        CacheEntry<V> entry = data.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (entry.isExpired(now)) {
            drop(key);
            return Optional.empty();
        }
        policy.keyAccessed(key);
        if (expiryMode == ExpiryMode.AFTER_ACCESS) {
            CacheEntry<V> renewed = entry.renewed(now);
            if (renewed != entry) {
                data.put(key, renewed);
            }
        }
        return Optional.of(entry.value());
    }

    /**
     * Writes with the cache-wide default TTL (or no expiry if none was set).
     */
    public synchronized void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    /**
     * Writes with a per-entry TTL, overriding the cache default. A null ttl
     * means this entry never expires, even if the cache has a default.
     */
    public synchronized void put(K key, V value, Duration ttl) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        validateTtl(ttl);

        Instant now = clock.instant();
        CacheEntry<V> entry = new CacheEntry<>(value, ttl, now);

        if (data.containsKey(key)) {
            // Overwrite refreshes the TTL: the entry's deadline is measured
            // from this write, so a hot key that keeps being rewritten never
            // goes stale. The policy sees an access, not an insert, so the
            // key keeps its identity in the ordering (and FIFO correctly
            // ignores it -- a rewrite is not a new arrival).
            data.put(key, entry);
            policy.keyAccessed(key);
            return;
        }

        if (data.size() == capacity) {
            // Reclaim dead entries before killing a live one. Skipping this
            // would let an entry that expired an hour ago push out a valid
            // key that was written a second ago -- correct by the letter of
            // the policy, indefensible in an interview.
            purgeExpired(now);
        }

        if (data.size() == capacity) {
            K victim = policy.selectEvictionCandidate();
            if (victim != null) {
                drop(victim);
            }
        }

        data.put(key, entry);
        policy.keyAdded(key);
    }

    public synchronized boolean remove(K key) {
        if (data.remove(key) == null) {
            return false;
        }
        policy.keyRemoved(key);
        return true;
    }

    /**
     * Eagerly deletes every expired entry and reports how many went. Exposed
     * because lazy expiry alone never reclaims an entry nobody touches again;
     * a caller that cares about memory can schedule this, which is the one
     * line of "sweeper" this design needs.
     */
    public synchronized int purgeExpired() {
        return purgeExpired(clock.instant());
    }

    /**
     * Number of entries currently stored. This counts entries that have
     * expired but have not been purged yet, because making size() exact would
     * mean an O(n) scan on what callers expect to be an O(1) call. Call
     * purgeExpired() first if you need the exact live count.
     */
    public synchronized int size() {
        return data.size();
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Single place where an entry leaves the cache, so the invariant "data
     * and policy always agree" has exactly one implementation to get right.
     */
    private void drop(K key) {
        data.remove(key);
        policy.keyRemoved(key);
    }

    /**
     * Every expired entry is judged against the SAME instant, so a purge can
     * never be internally inconsistent. Collecting the victims first avoids
     * mutating `data` while iterating it -- policy.keyRemoved is a call into
     * foreign code, and doing that mid-iteration is how ConcurrentModification
     * bugs are born.
     */
    private int purgeExpired(Instant now) {
        List<K> expired = new ArrayList<>();
        for (Iterator<Map.Entry<K, CacheEntry<V>>> it = data.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<K, CacheEntry<V>> e = it.next();
            if (e.getValue().isExpired(now)) {
                expired.add(e.getKey());
            }
        }
        for (K key : expired) {
            drop(key);
        }
        return expired.size();
    }

    private static void validateTtl(Duration ttl) {
        if (ttl != null && (ttl.isZero() || ttl.isNegative())) {
            throw new IllegalArgumentException("TTL must be positive");
        }
    }
}
