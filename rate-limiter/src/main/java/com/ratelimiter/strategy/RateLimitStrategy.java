package com.ratelimiter.strategy;

/**
 * Strategy interface that decides whether a SINGLE client's next request
 * should be allowed under that strategy's configured limit.
 *
 * Each instance of a strategy owns the bookkeeping for exactly one client
 * (one IP, one user-id, one API key, ...). The orchestrator RateLimiter
 * holds a Map<clientId, RateLimitStrategy> and looks up the per-client
 * instance on every call. This keeps the strategy interface tiny -- a
 * single allow() method -- and pushes all per-client state into the
 * strategy implementation where it naturally belongs.
 *
 * Implementations MUST be safe to call concurrently from multiple threads.
 * In practice this means synchronizing the body of allow() because every
 * known rate-limit algorithm (token bucket, leaky bucket, fixed window,
 * sliding window log, sliding window counter) is a read-then-write on
 * shared mutable state.
 */
public interface RateLimitStrategy {

    /**
     * Reports whether the caller is allowed to proceed RIGHT NOW. The
     * strategy updates its internal state as a side effect:
     *   - on allow  : consume a token / record this hit
     *   - on deny   : leave state unchanged
     *
     * The caller does not pass a timestamp. Each strategy reads the clock
     * itself so that callers cannot accidentally (or maliciously) replay
     * old timestamps to slip past the limit.
     */
    boolean allow();
}
