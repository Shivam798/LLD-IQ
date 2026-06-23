package com.ratelimiter.strategy;

/**
 * Fixed Window Counter rate limiter for a single client.
 *
 * Mental model: chop time into back-to-back, non-overlapping windows of
 * `windowMillis`. Inside each window we maintain ONE counter. Every
 * allowed request increments the counter; when it reaches `maxRequests`
 * we deny until the window rolls over.
 *
 *      |---- window 1 ----|---- window 2 ----|---- window 3 ----|
 *      [   counter = 3   ][   counter = 0   ][   counter = 0   ]
 *
 * This is the SIMPLEST algorithm in the family and is almost always the
 * first thing an interviewer asks you to implement. It is also the one
 * the interviewer will deliberately attack to push you toward the
 * sliding-window variants.
 *
 * The boundary burst bug (memorise this, the interviewer WILL probe):
 *   Suppose maxRequests = 100 and windowMillis = 60_000 (1 minute).
 *   A client fires 100 requests at second 59 of minute 1 (counter
 *   maxes out, then the window rolls), and another 100 at second 0
 *   of minute 2 (fresh window, counter restarts). Wall-clock effect:
 *   200 requests in roughly one second -- 2x the configured rate --
 *   even though each fixed window saw only 100. This is the bug that
 *   Sliding Window Log and Sliding Window Counter exist to fix.
 *
 * Pros:
 *   - O(1) memory per client: a counter + a window-start timestamp.
 *   - O(1) per call: no eviction loop, no logs.
 *   - Trivial to implement on top of Redis with INCR + EXPIRE.
 *
 * Cons:
 *   - The boundary burst bug above. For internal traffic this is often
 *     tolerable; for adversarial public APIs it is not.
 */
public class FixedWindowCounterStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final long windowMillis;

    // Start time of the CURRENT window. Whenever `now - windowStart`
    // crosses `windowMillis`, we roll: counter back to zero and the
    // window start advances. We deliberately advance by exact multiples
    // of windowMillis (rather than to `now`) so that long gaps don't
    // shift the boundaries -- otherwise two clients hitting the limiter
    // 30s apart would silently end up on different window grids.
    private long windowStart;
    private int count;

    public FixedWindowCounterStrategy(int maxRequests, long windowMillis) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.windowStart = System.currentTimeMillis();
        this.count = 0;
    }

    /**
     * Synchronized because the body is a read-modify-write on the
     * (windowStart, count) pair. Without the lock two threads at a
     * window boundary could both decide "still in old window, counter
     * == max, deny" while the actual state has already rolled -- or
     * worse, both roll and both reset the counter.
     */
    @Override
    public synchronized boolean allow() {
        long now = System.currentTimeMillis();
        long elapsed = now - windowStart;

        if (elapsed >= windowMillis) {
            // The window has rolled at least once. Snap forward by full
            // window multiples so the grid stays aligned and the new
            // window starts with a fresh counter.
            long windowsToSkip = elapsed / windowMillis;
            windowStart += windowsToSkip * windowMillis;
            count = 0;
        }

        if (count < maxRequests) {
            count++;
            return true;
        }
        return false;
    }
}
