package com.ratelimiter.model;

import com.ratelimiter.strategy.RateLimitStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-key rate limiter facade.
 *
 * The interesting decision here: each client (key) gets its OWN
 * RateLimitStrategy instance. The strategy holds the per-client state
 * (token count, hit log, ...), so the RateLimiter only needs a
 * Map<clientId, strategy> and a factory that knows how to build a fresh
 * strategy when a brand-new client shows up.
 *
 * Why a Supplier<RateLimitStrategy> factory and not a single shared
 * instance? Because every algorithm we support (token bucket, sliding
 * window, ...) keeps mutable per-client state. Sharing one instance
 * across all clients would mean one client's traffic could drain
 * another's tokens. The factory gives each client a fresh state
 * container while still letting them all use the same algorithm.
 *
 * ConcurrentHashMap + computeIfAbsent gives us thread-safe lazy creation
 * of per-client strategies without a global lock on the limiter. Each
 * strategy then synchronizes its own internal allow() body, so the only
 * contention is between threads hitting the SAME client -- which is
 * exactly the contention we want to limit anyway.
 */
public class RateLimiter {

    private final Supplier<RateLimitStrategy> strategyFactory;
    private final Map<String, RateLimitStrategy> perClient = new ConcurrentHashMap<>();

    public RateLimiter(Supplier<RateLimitStrategy> strategyFactory) {
        if (strategyFactory == null) {
            throw new IllegalArgumentException("strategyFactory is required");
        }
        this.strategyFactory = strategyFactory;
    }

    /**
     * "Is this client allowed to make a request right now?"
     *
     * - computeIfAbsent creates the per-client strategy on first sight
     *   and returns the existing one thereafter -- both paths are atomic
     *   under ConcurrentHashMap.
     * - The actual rate-limit check (synchronization, bookkeeping,
     *   clock reads) happens inside strategy.allow(), keeping this
     *   facade ignorant of the algorithm.
     */
    public boolean allow(String clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId cannot be null");
        }
        RateLimitStrategy strategy = perClient.computeIfAbsent(clientId, k -> strategyFactory.get());
        return strategy.allow();
    }
}
