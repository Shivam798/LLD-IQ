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
 *   As the current window fills, overlapFraction decays smoothly from
 *   1.0 (just rolled, previous counts fully) to 0.0 (previous about to
 *   drop off). That smooth decay, instead of Fixed Window's cliff-style
 *   reset, is exactly what kills the boundary-burst bug.
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

    // Which window are we currently inside? Computed as
    // `now / windowMillis` -- the exact same window id that
    // FixedWindowCounterStrategy uses. Because long division truncates,
    // two calls inside the same window get the same id -- which is
    // exactly what we want for cheap "did we cross a boundary?" checks.
    private long currentWindow;
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
        this.currentWindow = System.currentTimeMillis() / windowMillis;
        this.currentCount = 0;
        this.previousCount = 0;
    }

    /**
     * Synchronized because the window-roll logic and the count update
     * must be atomic. Without the lock two threads at a window boundary
     * could each independently roll the window, doubling the reset.
     */
    @Override
    public synchronized boolean allow() {
        long now = System.currentTimeMillis();

        // Step 1: catch up -- crossed into a new window? Same check as
        // Fixed Window, plus one extra line: if we moved exactly one
        // window, current becomes previous; if we skipped two or more
        // (idle client), both are stale and there is no "previous".
        long window = now / windowMillis;
        if (window != currentWindow) {
            previousCount = (window == currentWindow + 1) ? currentCount : 0;
            currentCount = 0;
            currentWindow = window;
        }

        // Step 2: check -- is the rolling estimate already at the limit?
        // The rolling view [now - windowMillis, now] holds all of the
        // current window plus the last `overlapFraction` of the previous
        // one (see the timeline in the class javadoc). Assuming the
        // previous window's hits were spread uniformly, that same fraction
        // of previousCount is still in view.
        //   e.g. 30% into current window -> overlapFraction = 0.7
        //        max 100, current 20, previous 80 -> 20 + 80 * 0.7 = 76 -> allow
        double overlapFraction = 1.0 - (double) (now % windowMillis) / windowMillis;
        double estimate = currentCount + previousCount * overlapFraction;
        if (estimate >= maxRequests) {
            return false;
        }

        // Step 3: consume -- record this request.
        currentCount++;
        return true;
    }
}
