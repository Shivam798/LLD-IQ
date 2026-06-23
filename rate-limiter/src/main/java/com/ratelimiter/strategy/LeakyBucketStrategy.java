package com.ratelimiter.strategy;

/**
 * Leaky Bucket rate limiter for a single client (counter form).
 *
 * Mental model: a bucket of `capacity` units that LEAKS at a constant
 * rate of `leakRatePerSecond`. Every incoming request pours one unit
 * of water into the bucket. If the bucket would OVERFLOW, the request
 * is denied; otherwise it is allowed.
 *
 *                          incoming requests
 *                                |
 *                                v
 *                             +-----+
 *                             |  *  |   <- water level (current "fullness")
 *                             |  *  |
 *                  capacity = |  *  |
 *                             |     |
 *                             +--V--+
 *                                |
 *                                v
 *                          leakRate / sec
 *
 * Two flavours of leaky bucket exist; this is the COUNTER form (also
 * called the "leaky bucket as a meter"). It tracks a numeric water
 * level and a last-leak timestamp -- no actual queue. The other
 * flavour, a literal FIFO queue drained by a worker thread, is the
 * "leaky bucket as a queue" and is what shapes outgoing traffic to a
 * perfectly uniform rate. We pick the counter form for an LLD round
 * because it has no background threads and slots cleanly into the
 * same Strategy interface as the others.
 *
 * Honest comparison with Token Bucket:
 *
 *   The counter-form leaky bucket is MATHEMATICALLY EQUIVALENT to a
 *   token bucket of the same capacity and rate. Token bucket asks
 *   "do I have a token to spend?"; leaky bucket asks "is there room
 *   in the bucket?". Same compare, same outcome.
 *
 *   The real difference is intent:
 *     - Token bucket says "you may burst up to `capacity`, then
 *       I'll throttle you to `refillRate`."
 *     - Leaky bucket says "your average inflow may not exceed
 *       `leakRate`, and I tolerate short overshoots up to `capacity`."
 *
 *   When the interviewer asks "but aren't these the same?" the
 *   correct answer is: counter-form, yes, basically. Queue-form, no --
 *   the queue smooths OUTPUT to a uniform rate; token bucket never
 *   reshapes the output, it only decides allow/deny.
 *
 * When to choose leaky bucket over token bucket:
 *   - You want the API to "feel" like traffic shaping (network ops,
 *     telecom, packet scheduling).
 *   - You want the natural intuition of "filling a bucket" rather than
 *     "spending tokens" -- some teams find it easier to reason about
 *     overflow than depletion.
 *
 * When NOT to choose it:
 *   - You actually need the burst budget framing -- token bucket is
 *     clearer.
 *   - You want a true FIFO queue with smoothed output -- you need the
 *     queue-form leaky bucket, which lives outside this strategy
 *     interface (it owns its own worker thread).
 */
public class LeakyBucketStrategy implements RateLimitStrategy {

    private final long capacity;
    private final double leakRatePerSecond;

    // How "full" the bucket is right now. Stored as a double so the
    // continuous leak math doesn't truncate to zero between fast calls
    // (same reason TokenBucketStrategy stores `tokens` as a double).
    private double water;

    // Monotonic clock anchor for the last leak computation. nanoTime
    // never goes backwards, which keeps the leak math from producing
    // negative deltas on NTP adjustments.
    private long lastLeakNanos;

    public LeakyBucketStrategy(long capacity, double leakRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (leakRatePerSecond <= 0) {
            throw new IllegalArgumentException("leakRatePerSecond must be positive");
        }
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
        // Start EMPTY -- opposite of token bucket's "start full". An
        // empty bucket means "no debt", which is the correct initial
        // state for a leaky-bucket meter: a fresh client owes us
        // nothing, so they have the full burst budget available.
        this.water = 0.0;
        this.lastLeakNanos = System.nanoTime();
    }

    /**
     * Synchronized because leak + room-check + fill is the classic
     * read-modify-write. Two unsynchronized threads could both observe
     * water == capacity - 0.5, both decide there is room for +1, and
     * both fill -- overshooting the bucket.
     */
    @Override
    public synchronized boolean allow() {
        leak();
        if (water + 1.0 <= capacity) {
            water += 1.0;
            return true;
        }
        return false;
    }

    /**
     * Drain the bucket by however much would have leaked since the
     * last call, floored at zero. Lazy leak: no background thread,
     * just compute on demand -- same trick as TokenBucketStrategy's
     * refill.
     */
    private void leak() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastLeakNanos;
        if (elapsedNanos <= 0) {
            return;
        }
        double leaked = (elapsedNanos / 1_000_000_000.0) * leakRatePerSecond;
        water = Math.max(0.0, water - leaked);
        lastLeakNanos = now;
    }
}
