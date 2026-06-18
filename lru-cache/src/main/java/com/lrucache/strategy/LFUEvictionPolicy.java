package com.lrucache.strategy;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Least Frequently Used eviction, with LRU as tiebreaker between keys that
 * share the same frequency.
 *
 * Bookkeeping:
 *   keyFreq      : key -> access count
 *   freqBuckets  : frequency -> LinkedHashSet of keys at that frequency
 *                  (LinkedHashSet preserves insertion order, giving LRU
 *                  tiebreak when several keys share the same count)
 *   minFreq      : smallest frequency currently in freqBuckets
 *
 * All hooks are O(1) amortized. On access the key moves from bucket[f] to
 * bucket[f+1]. On eviction we drop the first element of bucket[minFreq] --
 * that key has the lowest count, and within that count it is the oldest.
 */
public class LFUEvictionPolicy<K> implements EvictionPolicy<K> {

    // key -> current access count. Lets us look up "how many times has this
    // key been hit?" in O(1) so we know which bucket to remove it from on
    // the next access.
    private final Map<K, Integer> keyFreq = new HashMap<>();

    // frequency -> all keys that currently have that frequency.
    //
    // LinkedHashSet, NOT plain HashSet. Reason: when several keys share the
    // same frequency, we still need a deterministic eviction order, and the
    // industry convention is LRU-within-frequency. LinkedHashSet preserves
    // insertion order, so iterator().next() returns the key that entered
    // this bucket first -- i.e. the one that has been sitting at this
    // frequency the longest. Plain HashSet gives arbitrary order and would
    // make eviction non-deterministic.
    private final Map<Integer, LinkedHashSet<K>> freqBuckets = new HashMap<>();

    // The smallest frequency currently present in any bucket. We track it
    // as a field so selectEvictionCandidate is O(1). Without it, we'd have
    // to scan freqBuckets.keySet() on every eviction to find the minimum.
    //
    // Invariant: if freqBuckets is non-empty, minFreq == min(keys in freqBuckets).
    //            if freqBuckets is empty, minFreq is conceptually undefined
    //            (we reset it to 0 in recomputeMinFreq for sanity).
    private int minFreq = 0;

    /**
     * Brand new key. By convention it enters at frequency 1 -- not 0 --
     * because the very act of inserting it counts as the first "use".
     *
     * minFreq is reset to 1 here because the new key now sits in bucket[1],
     * and bucket[1] is by definition the smallest possible bucket. Any
     * older key would either still be in bucket[1] (so minFreq was already
     * 1) or be in a higher bucket (in which case the new key is now the
     * smallest -- minFreq must come down to 1).
     */
    @Override
    public void keyAdded(K key) {
        keyFreq.put(key, 1);
        // computeIfAbsent creates bucket[1] lazily the first time we need
        // it. Avoids carrying empty buckets around.
        freqBuckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    /**
     * Existing key was read or its value was updated. Promote it from
     * bucket[f] to bucket[f+1] and bump its count.
     *
     * The tricky bit is keeping minFreq correct without a scan. The trick:
     * if we just emptied bucket[minFreq], we can safely set minFreq = f+1
     * because the key we just promoted landed in bucket[f+1], and every
     * other key in the cache had freq >= old minFreq anyway. So f+1 is
     * either the new minimum or a strict lower bound on it that gets
     * tightened the next time someone is added to a smaller bucket.
     */
    @Override
    public void keyAccessed(K key) {
        Integer f = keyFreq.get(key);
        if (f == null) {
            // Cache has no idea this key exists. Probably a stale call
            // after eviction -- tolerate it silently.
            return;
        }

        // Step 1: yank the key out of its current frequency bucket.
        LinkedHashSet<K> bucket = freqBuckets.get(f);
        bucket.remove(key);

        // Step 2: if that emptied the bucket entirely, drop the bucket.
        // Empty buckets are kept out of the map so we never accidentally
        // pick an empty bucket during eviction.
        if (bucket.isEmpty()) {
            freqBuckets.remove(f);
            // If the emptied bucket WAS the current minimum, the new
            // minimum is exactly f+1 -- see the method-level comment for
            // why this is safe without a scan.
            if (minFreq == f) {
                minFreq++;
            }
        }

        // Step 3: bump the count and drop the key into bucket[f+1]. The
        // key now sits at the *back* of the new bucket (LinkedHashSet
        // insertion order), which is correct -- it's the most recently
        // promoted key at this frequency level.
        int newFreq = f + 1;
        keyFreq.put(key, newFreq);
        freqBuckets.computeIfAbsent(newFreq, k -> new LinkedHashSet<>()).add(key);
    }

    /**
     * Explicit removal (manual cache.remove() or eviction). Symmetric to
     * keyAccessed's first half, but with no promotion at the end.
     *
     * IMPORTANT difference from keyAccessed: when we empty bucket[minFreq]
     * here, we CANNOT just do `minFreq++`. The key was not promoted -- it
     * was deleted entirely -- so the next smallest bucket could be at any
     * frequency above f, not necessarily f+1. We have to scan to find it.
     * recomputeMinFreq() handles that. This is the only non-O(1) path in
     * the whole policy; manual removes are rare so the trade-off is fine.
     */
    @Override
    public void keyRemoved(K key) {
        Integer f = keyFreq.remove(key);
        if (f == null) {
            return;
        }
        LinkedHashSet<K> bucket = freqBuckets.get(f);
        if (bucket == null) {
            // Defensive: keyFreq and freqBuckets should always agree, but
            // if somehow they don't, bail out instead of NPE'ing.
            return;
        }
        bucket.remove(key);
        if (bucket.isEmpty()) {
            freqBuckets.remove(f);
            if (minFreq == f) {
                recomputeMinFreq();
            }
        }
    }

    /**
     * "Who should we evict?" The answer is "any key with the smallest
     * frequency, oldest-first within that frequency."
     *
     *   bucket = freqBuckets.get(minFreq)    -- smallest frequency
     *   bucket.iterator().next()             -- oldest in that bucket
     *                                            (LinkedHashSet preserves
     *                                             insertion order)
     *
     * Both operations are O(1). We don't remove the key here -- we just
     * report the candidate. The Cache will call keyRemoved after deleting
     * the value from its HashMap.
     */
    @Override
    public K selectEvictionCandidate() {
        LinkedHashSet<K> bucket = freqBuckets.get(minFreq);
        if (bucket == null || bucket.isEmpty()) {
            // Nothing to evict -- cache is empty.
            return null;
        }
        return bucket.iterator().next();
    }

    /**
     * Walk every frequency currently present and find the smallest.
     *
     * This is O(distinct frequencies), not O(1). Called ONLY from
     * keyRemoved when removing a key empties bucket[minFreq]. It is never
     * called on the hot keyAccessed / selectEvictionCandidate paths, so
     * the cache's amortized cost stays O(1) per operation.
     *
     * If your workload does lots of manual removes, replace freqBuckets
     * with a TreeMap<Integer, LinkedHashSet<K>> and use firstKey() here --
     * that gives O(log n) but kills the scan.
     */
    private void recomputeMinFreq() {
        if (freqBuckets.isEmpty()) {
            // Cache is empty. Reset to a sentinel value; minFreq will be
            // set properly the next time keyAdded fires.
            minFreq = 0;
            return;
        }
        int newMin = Integer.MAX_VALUE;
        for (Integer f : freqBuckets.keySet()) {
            newMin = Math.min(newMin, f);
        }
        minFreq = newMin;
    }
}
