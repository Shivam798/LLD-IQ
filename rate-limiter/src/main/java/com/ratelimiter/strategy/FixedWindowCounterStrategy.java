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

    // Which window are we currently inside? Computed as
    // `now / windowMillis`, so every call inside the same window gets
    // the same id and the windows sit on the wall-clock grid (:00, :01,
    // ...) for EVERY client -- the same id you would use as the Redis key
    // (`client:<id>:<window>` + INCR + EXPIRE) in a distributed version.
    // Integer division also jumps straight past any number of idle
    // windows, so no "how many windows did we skip?" arithmetic.
    private long currentWindow;
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
        this.currentWindow = System.currentTimeMillis() / windowMillis;
        this.count = 0;
    }

    /**
     * Synchronized because the body is a read-modify-write on the
     * (currentWindow, count) pair. Without the lock two threads at a
     * window boundary could both decide "still in old window, counter
     * == max, deny" while the actual state has already rolled -- or
     * worse, both roll and both reset the counter.
     */
    @Override
    public synchronized boolean allow() {
        long now = System.currentTimeMillis();

        // Step 1: catch up -- crossed into a new window? Fresh counter.
        long window = now / windowMillis;
        if (window != currentWindow) {
            currentWindow = window;
            count = 0;
        }

        // Step 2: check -- window already full?
        if (count >= maxRequests) {
            return false;
        }

        // Step 3: consume -- record this request.
        count++;
        return true;
    }
}
