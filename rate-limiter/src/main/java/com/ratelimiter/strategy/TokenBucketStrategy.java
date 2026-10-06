package com.ratelimiter.strategy;

/**
 * Token Bucket rate limiter for a single client.
 *
 * Mental model: a bucket of `capacity` tokens that refills at a constant
 * rate of `refillRatePerSecond` tokens per second. Every request consumes
 * one token; if no token is available, the request is denied.
 *
 *                     refillRatePerSecond
 *                             |
 *                             v
 *                          +-----+
 *                          | o o |   <- tokens (starts full, never
 *                          | o o |      exceeds `capacity`)
 *                          +--+--+
 *                             |
 *                             v
 *                  each request takes one token
 *
 * Why token bucket is a favourite in interviews:
 *  - Allows BURSTS up to `capacity` (unlike leaky bucket which strictly
 *    smooths) -- realistic for traffic that's spiky but bounded.
 *  - O(1) memory per client: two numbers (tokens, lastRefillNanos). No
 *    per-request log to maintain.
 *  - O(1) per call: a subtract and a compare.
 *
 * Lazy refill: we do NOT spawn a background thread to top up the bucket.
 * On every allow() we compute "how many tokens *would* have been added
 * since the last call" and add them in one shot, capped at `capacity`.
 * This is mathematically identical to a continuous refill but uses zero
 * threads and zero scheduling overhead -- a win for any LLD interview.
 */
public class TokenBucketStrategy implements RateLimitStrategy {

    private final long capacity;
    private final double refillRatePerSecond;

    // We store tokens as a double, not a long, so that fractional tokens
    // accumulate cleanly between calls. Example: 10 tokens/sec, two calls
    // 50ms apart -- we should add 0.5 tokens, then 0.5 more on the next
    // call. With longs we'd round down to 0 each time and lose the rate.
    private double tokens;

    // nanoTime, not currentTimeMillis: we only need ELAPSED time, and
    // nanoTime is monotonic -- it cannot go backwards on NTP adjustments.
    // A wall-clock jump back would make elapsed negative and silently
    // remove tokens.
    private long lastRefillNanos;

    public TokenBucketStrategy(long capacity, double refillRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be positive");
        }
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        // Start full so the very first burst is allowed up to `capacity`.
        // Starting empty would mean every fresh client has to wait one
        // refill interval before any request -- almost never what we want.
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Synchronized because the body is a classic read-modify-write on
     * shared mutable state (`tokens`, `lastRefillNanos`). Without the
     * lock two threads could both observe `tokens == 1`, both decrement
     * to `0`, and both be allowed -- one request slips past the limit.
     */
    @Override
    public synchronized boolean allow() {
        long now = System.nanoTime();

        // Step 1: catch up -- lazy refill for the time since the last
        // call. The cap matters: a client idle for hours gets at most
        // `capacity` tokens back, not a million.
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);
        lastRefillNanos = now;

        // Step 2: check -- no whole token left?
        if (tokens < 1.0) {
            return false;
        }

        // Step 3: consume -- spend one token.
        tokens -= 1.0;
        return true;
    }
}
