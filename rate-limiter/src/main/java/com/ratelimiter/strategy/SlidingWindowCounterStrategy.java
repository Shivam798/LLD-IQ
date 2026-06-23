package com.ratelimiter.strategy;

/**
 * Sliding Window COUNTER rate limiter for a single client.
 *
 * The hybrid: cheaper than Sliding Window Log, more accurate than
 * Fixed Window. This is the algorithm Cloudflare describes in their
 * famous blog post and the one most large APIs actually run in
 * production behind the scenes.
 *
 * Mental model: keep TWO fixed-window counters -- the one we are
 * currently inside, and the one that ended right before it. To answer
 * "is the rolling window full?" we take a weighted blend:
 *
 *      estimate = currentCount + previousCount * overlapFraction
 *
 * where `overlapFraction` is the portion of the previous window that
 * still falls inside the rolling [now - windowMillis, now] view.
 *
 *      |--- previous ---|--- current ---|
 *                  ^ now is here, say 30% into current window
 *                  rolling window covers the last 70% of previous + 30% of current
 *                  so overlapFraction = 0.7
 *                  estimate = currentCount + 0.7 * previousCount
 *
 * Why this works (intuition):
 *   We assume the previous window's requests were spread uniformly
 *   across that window. So 70% of `previousCount` "still counts" toward
 *   the rolling view, plus everything we've seen so far in the current
 *   window. It's a linear interpolation between two fixed-window counts.
 *
 * Accuracy:
 *   - Worst case error vs. true sliding-window-log: ~0.003% on average
 *     for typical traffic, can spike if traffic is hyper-bursty inside
 *     the previous window. Acceptable for almost every real workload.
 *
 * Memory & CPU:
 *   - O(1) memory per client: two ints + one long. No deque, no logs.
 *   - O(1) per call: two multiplies, one compare. No eviction loop.
 *
 * This is the algorithm to reach for when:
 *   - Sliding Window Log's memory cost is unacceptable (millions of
 *     clients, each with thousands of req/window).
 *   - Fixed Window's boundary burst bug is unacceptable (public-facing
 *     APIs, billing meters, anti-abuse).
 *
 * It is, frankly, the right answer for most real-world rate limiters.
 */
public class SlidingWindowCounterStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final long windowMillis;

    // Which fixed-window bucket are we currently inside? Computed as
    // `now / windowMillis`. Because Long division truncates, two calls
    // inside the same window get the same bucket id -- which is exactly
    // what we want for cheap "did we cross a boundary?" checks.
    private long currentBucket;
    private int currentCount;
    private int previousCount;

    public SlidingWindowCounterStrategy(int maxRequests, long windowMillis) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.currentBucket = System.currentTimeMillis() / windowMillis;
        this.currentCount = 0;
        this.previousCount = 0;
    }

    /**
     * Synchronized because the bucket-roll logic and the count update
     * must be atomic. Without the lock two threads at a window boundary
     * could each independently roll the bucket, doubling the reset.
     */
    @Override
    public synchronized boolean allow() {
        long now = System.currentTimeMillis();
        long bucket = now / windowMillis;

        // Step 1: catch up the bucket pointer if we have crossed a
        // window boundary since the last call.
        if (bucket == currentBucket + 1) {
            // Advanced exactly one window: current becomes previous.
            previousCount = currentCount;
            currentCount = 0;
            currentBucket = bucket;
        } else if (bucket > currentBucket + 1) {
            // Skipped two or more windows (idle client). Both buckets
            // are now stale -- there is no "previous" anymore.
            previousCount = 0;
            currentCount = 0;
            currentBucket = bucket;
        }

        // Step 2: compute the rolling estimate.
        //
        // We never stored individual timestamps -- we only have two bucket
        // counts. To answer "how many requests in the last windowMillis?"
        // we LINEARLY INTERPOLATE between them, assuming the previous
        // window's hits were spread uniformly across it.

        // (a) How far are we into the current window?
        // currentBucket * windowMillis = start time of the current window;
        // subtracting from `now` gives ms consumed inside the current window.
        // e.g. windowMillis = 60_000, currentBucket = 100, now is 18s into
        // that bucket  ->  elapsedInCurrent = 18_000.
        long elapsedInCurrent = now - currentBucket * windowMillis;

        // (b) Fraction of the PREVIOUS window still in the rolling view
        //     [now - windowMillis, now].
        //
        //   elapsedInCurrent / windowMillis = fraction of current window already consumed.
        //   `1 - that`                      = fraction of previous window still in view.
        //
        // Timeline (30% into current window):
        //   |--- previous ---|--- current ---|
        //                ^                ^
        //                |                now
        //                start of rolling view = now - windowMillis
        //   -> overlapFraction = 0.70 (last 70% of previous is still visible,
        //      first 30% has already slid out).
        //
        // Edge cases:
        //   elapsedInCurrent = 0            -> overlap = 1.0 (just rolled, previous counts fully)
        //   elapsedInCurrent ~ windowMillis -> overlap -> 0.0 (previous about to drop off)
        double overlapFraction = 1.0 - ((double) elapsedInCurrent / windowMillis);

        // (c) Blend the two counts.
        //   currentCount  -> every hit in current window is FULLY in view, counts as-is.
        //   previousCount -> only `overlapFraction` of it is still in view, and under
        //                    the uniform-distribution assumption that same fraction
        //                    of its hits are in view.
        //
        // This is what makes the limit "slide" instead of resetting cliff-style at
        // the bucket boundary -- the previous window's contribution decays smoothly
        // toward zero as the current window fills. That decay is exactly what kills
        // the Fixed Window boundary-burst bug.
        //
        // Worked example, maxRequests = 100:
        //   currentCount = 20, previousCount = 80, overlap = 0.70
        //   estimate = 20 + 80 * 0.70 = 76  -> under limit, allow.
        double estimate = currentCount + previousCount * overlapFraction;

        // Step 3: allow or deny based on the blended count.
        if (estimate < maxRequests) {
            currentCount++;
            return true;
        }
        return false;
    }
}
