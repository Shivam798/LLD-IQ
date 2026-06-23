package com.ratelimiter;

import com.ratelimiter.model.RateLimiter;
import com.ratelimiter.strategy.FixedWindowCounterStrategy;
import com.ratelimiter.strategy.LeakyBucketStrategy;
import com.ratelimiter.strategy.SlidingWindowCounterStrategy;
import com.ratelimiter.strategy.SlidingWindowLogStrategy;
import com.ratelimiter.strategy.TokenBucketStrategy;

/**
 * Walks through all five strategies in the order an interviewer
 * typically asks for them. Each demo prints the algorithm's behaviour
 * under a small burst so you can see the differences side-by-side.
 *
 * Recommended reading order (same as the README's Interview Flow):
 *   1. Fixed Window Counter      -- simplest baseline
 *   2. Sliding Window Log        -- fixes the boundary burst bug
 *   3. Sliding Window Counter    -- O(1) memory hybrid
 *   4. Token Bucket              -- burst-friendly production default
 *   5. Leaky Bucket              -- token bucket's traffic-shaping cousin
 */
public class RateLimiterDemo {

    public static void main(String[] args) throws InterruptedException {
        runFixedWindow();
        System.out.println();
        runSlidingWindowLog();
        System.out.println();
        runSlidingWindowCounter();
        System.out.println();
        runTokenBucket();
        System.out.println();
        runLeakyBucket();
    }

    // 1) Fixed Window -----------------------------------------------------
    private static void runFixedWindow() throws InterruptedException {
        System.out.println("=== 1. Fixed Window Counter (3 requests per 1000 ms) ===");
        RateLimiter limiter = new RateLimiter(() -> new FixedWindowCounterStrategy(3, 1000));

        // First 3 within the window are allowed, next 2 denied.
        for (int i = 1; i <= 5; i++) {
            System.out.println("client-A request " + i + " -> " + (limiter.allow("client-A") ? "ALLOW" : "DENY"));
        }

        // After the window rolls, the counter resets in full -- this is
        // exactly the boundary that the sliding-window variants will fix.
        Thread.sleep(1100);
        System.out.println("--- after 1.1s sleep (window rolled, counter reset) ---");
        for (int i = 6; i <= 8; i++) {
            System.out.println("client-A request " + i + " -> " + (limiter.allow("client-A") ? "ALLOW" : "DENY"));
        }
    }

    // 2) Sliding Window Log -----------------------------------------------
    private static void runSlidingWindowLog() throws InterruptedException {
        System.out.println("=== 2. Sliding Window Log (3 requests per 1000 ms) ===");
        RateLimiter limiter = new RateLimiter(() -> new SlidingWindowLogStrategy(3, 1000));

        for (int i = 1; i <= 5; i++) {
            System.out.println("client-B request " + i + " -> " + (limiter.allow("client-B") ? "ALLOW" : "DENY"));
        }

        Thread.sleep(1100);
        System.out.println("--- after 1.1s sleep (window slid past earlier hits) ---");
        for (int i = 6; i <= 8; i++) {
            System.out.println("client-B request " + i + " -> " + (limiter.allow("client-B") ? "ALLOW" : "DENY"));
        }
    }

    // 3) Sliding Window Counter -------------------------------------------
    private static void runSlidingWindowCounter() throws InterruptedException {
        System.out.println("=== 3. Sliding Window Counter (3 requests per 1000 ms) ===");
        RateLimiter limiter = new RateLimiter(() -> new SlidingWindowCounterStrategy(3, 1000));

        for (int i = 1; i <= 5; i++) {
            System.out.println("client-C request " + i + " -> " + (limiter.allow("client-C") ? "ALLOW" : "DENY"));
        }

        // Wait ~30% into the next bucket: the rolling estimate is
        //   currentCount + previousCount * 0.7
        // so we still owe ~3 * 0.7 = ~2.1 from the previous bucket and
        // only ~0.9 of the new bucket is "free". The interviewer loves
        // this -- it's the algorithm's defining behaviour.
        Thread.sleep(1300);
        System.out.println("--- after 1.3s sleep (previous-window weight ~0.7) ---");
        for (int i = 6; i <= 9; i++) {
            System.out.println("client-C request " + i + " -> " + (limiter.allow("client-C") ? "ALLOW" : "DENY"));
        }
    }

    // 4) Token Bucket -----------------------------------------------------
    private static void runTokenBucket() throws InterruptedException {
        System.out.println("=== 4. Token Bucket (capacity 5, refill 2 tokens/sec) ===");
        // capacity=5 allows a burst of 5; refill of 2/sec means the
        // steady-state ceiling is 2 req/sec/client.
        RateLimiter limiter = new RateLimiter(() -> new TokenBucketStrategy(5, 2.0));

        // Burst of 7: first 5 allowed (full bucket), next 2 denied.
        for (int i = 1; i <= 7; i++) {
            System.out.println("client-D request " + i + " -> " + (limiter.allow("client-D") ? "ALLOW" : "DENY"));
        }

        // Independent client still has a full bucket.
        System.out.println("client-E request 1 -> " + (limiter.allow("client-E") ? "ALLOW" : "DENY"));

        // Wait 1.5s: ~3 tokens refill for client-D.
        Thread.sleep(1500);
        System.out.println("--- after 1.5s sleep (client-D regains ~3 tokens) ---");
        for (int i = 8; i <= 11; i++) {
            System.out.println("client-D request " + i + " -> " + (limiter.allow("client-D") ? "ALLOW" : "DENY"));
        }
    }

    // 5) Leaky Bucket -----------------------------------------------------
    private static void runLeakyBucket() throws InterruptedException {
        System.out.println("=== 5. Leaky Bucket (capacity 5, leak 2 units/sec) ===");
        // Mirror of the token-bucket demo so you can compare the two.
        // Starts EMPTY: first burst of 5 fills it, next requests overflow.
        RateLimiter limiter = new RateLimiter(() -> new LeakyBucketStrategy(5, 2.0));

        for (int i = 1; i <= 7; i++) {
            System.out.println("client-F request " + i + " -> " + (limiter.allow("client-F") ? "ALLOW" : "DENY"));
        }

        Thread.sleep(1500);
        System.out.println("--- after 1.5s sleep (~3 units leaked out) ---");
        for (int i = 8; i <= 11; i++) {
            System.out.println("client-F request " + i + " -> " + (limiter.allow("client-F") ? "ALLOW" : "DENY"));
        }
    }
}
