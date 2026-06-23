package com.ratelimiter.strategy;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding Window LOG rate limiter for a single client.
 *
 * Keeps a Deque of timestamps -- one entry per allowed request inside
 * the rolling window. On every call:
 *   1. Evict timestamps older than `now - windowMillis` (they have
 *      slid out of the window).
 *   2. If the remaining count is below `maxRequests`, record `now`
 *      and allow. Otherwise deny.
 *
 *      |----- windowMillis -----|
 *      [ x   x  x    x  ][  current window ]
 *      ^ already evicted ^      ^ live hits ^
 *
 * Why "Log"?
 *   We literally keep a LOG of every allowed timestamp. That is what
 *   gives us exact precision -- we can answer "how many requests in
 *   the last N milliseconds?" by counting entries, no approximation.
 *   The cousin algorithm, SlidingWindowCounterStrategy, throws away
 *   individual timestamps and keeps just two bucket counts -- cheaper
 *   memory, slight accuracy loss.
 *
 * Why sliding window vs fixed window?
 *
 * The naive "fixed window" counter (reset every minute) has a well-known
 * burst bug: a client can do `maxRequests` at second 59 of minute 1 and
 * another `maxRequests` at second 0 of minute 2 -- 2 * maxRequests in a
 * one-second wall-clock interval, but each fixed window sees only
 * maxRequests. Sliding window eliminates this because the "minute" is
 * always anchored to "right now", not to a wall-clock boundary.
 *
 * Trade-offs vs Token Bucket:
 *   - More precise (no bucket burstiness; exactly N requests per window)
 *   - Higher memory: stores up to `maxRequests` timestamps per client
 *   - Pays an extra O(k) on eviction, but k is small and amortized O(1)
 *     because each timestamp is added once and evicted once
 */
public class SlidingWindowLogStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final long windowMillis;

    // ArrayDeque is faster than LinkedList for this access pattern --
    // we only ever poll-first (evict old) and offer-last (record new).
    // It also keeps memory tight: a single contiguous array vs. a
    // boxed Long node per timestamp in LinkedList.
    private final Deque<Long> hitTimestamps = new ArrayDeque<>();

    public SlidingWindowLogStrategy(int maxRequests, long windowMillis) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * Synchronized because evict + size-check + record is a compound
     * transaction. Two unsynchronized threads could both see size = N-1,
     * both record their hits, and overshoot the limit.
     *
     * We use System.currentTimeMillis (wall-clock) rather than nanoTime
     * here because the window is configured in millis and the absolute
     * value is what defines "old vs new" -- not a delta.
     *
     * Caveat: if NTP rewinds the clock, a single batch of evictions can
     * be skipped. For interview purposes this is acceptable; a hardened
     * production implementation would use a monotonic source plus a
     * separate epoch anchor.
     */
    @Override
    public synchronized boolean allow() {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;

        // Step 1: slide the window forward by dropping timestamps that
        // are now older than the cutoff. The deque is naturally ordered
        // by time (we only ever append `now`, which monotonically
        // increases), so we can stop the moment we see a fresh enough
        // entry -- no scan of the whole deque.
        while (!hitTimestamps.isEmpty() && hitTimestamps.peekFirst() <= cutoff) {
            hitTimestamps.pollFirst();
        }

        // Step 2: capacity check on the remaining entries.
        if (hitTimestamps.size() >= maxRequests) {
            return false;
        }

        // Step 3: record this hit at the tail and allow.
        hitTimestamps.offerLast(now);
        return true;
    }
}
