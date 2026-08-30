package com.lrucache.strategy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Evicts the key whose deadline is NEAREST -- the entry that is about to
 * expire anyway, so losing it costs the least.
 *
 * READ THIS FIRST: what this policy is and is NOT
 * -----------------------------------------------
 * This does NOT enforce expiry. An entry is never served past its deadline
 * because Cache checks CacheEntry.isExpired on every read, whether or not the
 * cache is full -- that is a correctness rule and it lives in Cache. This
 * policy answers a completely different question: "we are FULL and somebody
 * must go -- who?" It answers "whoever dies soonest."
 *
 *   Cache + CacheEntry  : "is this entry still TRUE?"     (always enforced)
 *   TTLEvictionPolicy   : "who leaves when we are FULL?"  (only under pressure)
 *
 * The two compose. Pair them and a full cache first purges what is already
 * dead, then sacrifices whatever was going to die next.
 *
 * Structure -- the same shape as LFU, one level up
 * ------------------------------------------------
 *   deadlines  : key -> its expiry Instant       (reverse index, exactly like LFU's keyFreq)
 *   byDeadline : Instant -> LinkedHashSet of keys sharing that deadline
 *                (LinkedHashSet again, so keys with the SAME deadline are
 *                 evicted oldest-arrival-first -- a FIFO tiebreak)
 *
 * Why a TreeMap and not HashMap + a tracked minimum, the way LFU does it?
 * Because LFU's `minFreq++` trick only works when the buckets are DENSE
 * integers that advance by exactly one: promoting a key from bucket[f] always
 * lands it in bucket[f+1], so the new minimum is knowable without looking.
 * Deadlines are arbitrary Instants -- after the earliest one empties, the next
 * could be a millisecond later or a week later, and nothing lands anywhere
 * predictable. There is no "+1" to take, so the minimum must come from a
 * structure that keeps itself sorted. That is the whole trade:
 *
 *   LFU  : HashMap + minFreq -> O(1) hooks, but needs recomputeMinFreq() repair
 *   TTL  : TreeMap           -> O(log n) hooks, but never needs repair at all
 *
 * Complexity: keyAdded / keyRemoved / selectEvictionCandidate are O(log n) in
 * the number of DISTINCT deadlines. keyAccessed is O(1) (it does nothing).
 * That is a genuine step down from the O(1) of LRU/LFU/FIFO, and it is not
 * avoidable: "give me the smallest of an arbitrary set of deadlines" is a
 * priority-queue problem, and priority queues cost log n. Say so out loud
 * rather than claiming O(1).
 *
 * The one case where it IS O(1): if every key shares the same TTL, deadlines
 * increase monotonically with arrival, so nearest-deadline == oldest-arrival
 * and this policy degenerates into plain FIFO. Use FIFOEvictionPolicy there --
 * it gets the same answer for free. This policy earns its log n only when
 * different keys have genuinely different lifetimes.
 */
public class TTLEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Clock clock;

    // Per-key lifetime. A Function rather than a single Duration because a
    // uniform TTL makes this policy identical to FIFO (see class javadoc);
    // it only earns its keep when "session:*" lives 30 minutes and
    // "config:*" lives 30 seconds.
    private final Function<? super K, Duration> ttlResolver;

    // deadline -> keys expiring at that instant. Sorted, so firstKey() is the
    // earliest deadline. LinkedHashSet gives arrival-order tiebreak when two
    // keys happen to land on the same instant (common with coarse clocks).
    private final TreeMap<Instant, LinkedHashSet<K>> byDeadline = new TreeMap<>();

    // key -> its deadline. The reverse index, and it exists for exactly the
    // reason LFU's keyFreq does: keyRemoved is handed a key and must find
    // which bucket holds it. Without this it would scan every bucket.
    private final Map<K, Instant> deadlines = new HashMap<>();

    /**
     * Every key gets the same lifetime. Note this is the degenerate case --
     * it behaves exactly like FIFO, at O(log n) instead of O(1). Useful for
     * demonstrating the equivalence; prefer FIFOEvictionPolicy in real code.
     */
    public TTLEvictionPolicy(Duration uniformTtl, Clock clock) {
        this(key -> uniformTtl, clock);
        validateTtl(uniformTtl);
    }

    /**
     * Lifetime derived from the key itself, so different classes of key can
     * have different deadlines. This is the case the policy is built for.
     */
    public TTLEvictionPolicy(Function<? super K, Duration> ttlResolver, Clock clock) {
        if (ttlResolver == null) {
            throw new IllegalArgumentException("ttlResolver is required");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        this.ttlResolver = ttlResolver;
        this.clock = clock;
    }

    /**
     * Stamp the key with an absolute deadline and file it under that instant.
     * Absolute rather than a stored duration for the same reason CacheEntry
     * does it: comparisons stay arithmetic-free and independent of when the
     * check runs.
     */
    @Override
    public void keyAdded(K key) {
        Duration ttl = ttlResolver.apply(key);
        validateTtl(ttl);
        Instant deadline = clock.instant().plus(ttl);
        deadlines.put(key, deadline);
        byDeadline.computeIfAbsent(deadline, d -> new LinkedHashSet<>()).add(key);
    }

    /**
     * Deliberately empty -- expire-after-WRITE semantics. Reading an entry
     * does not buy it more life, so a key's position never changes once
     * stamped. Same reasoning as FIFO's no-op, one dimension over: FIFO
     * ignores reads because it orders by arrival, this ignores reads because
     * it orders by deadline, and a read moves neither.
     *
     * Re-stamping here would give expire-after-ACCESS instead, which is a
     * legitimate variant (Guava exposes both) but a different policy: it
     * turns into "LRU with a clock", and a key that is polled forever would
     * never become evictable.
     */
    @Override
    public void keyAccessed(K key) {
        // no-op by design -- see javadoc
    }

    /**
     * Symmetric to keyAdded. Note what is NOT here: no equivalent of LFU's
     * recomputeMinFreq(). A TreeMap re-derives its own minimum on the next
     * firstEntry() call, so emptying the earliest bucket needs no repair --
     * that is what the O(log n) buys.
     */
    @Override
    public void keyRemoved(K key) {
        Instant deadline = deadlines.remove(key);
        if (deadline == null) {
            // Not tracked -- a stale call after eviction. Tolerate silently,
            // same as every other policy here.
            return;
        }
        LinkedHashSet<K> bucket = byDeadline.get(deadline);
        if (bucket == null) {
            return;
        }
        bucket.remove(key);
        if (bucket.isEmpty()) {
            // Drop empty buckets so firstEntry() can never hand back an
            // empty set.
            byDeadline.remove(deadline);
        }
    }

    /**
     * "Who should we evict?" Whoever expires soonest -- already-expired keys
     * sort first, so if anything is dead it goes before anything living.
     * O(log n) for the TreeMap descent; the tiebreak within one instant is
     * O(1) and picks the oldest arrival.
     */
    @Override
    public K selectEvictionCandidate() {
        Map.Entry<Instant, LinkedHashSet<K>> earliest = byDeadline.firstEntry();
        if (earliest == null) {
            return null;
        }
        return earliest.getValue().iterator().next();
    }

    private static void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("TTL must be positive");
        }
    }
}
