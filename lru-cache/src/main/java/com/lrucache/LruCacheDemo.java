package com.lrucache;

import com.lrucache.model.Cache;
import com.lrucache.strategy.FIFOEvictionPolicy;
import com.lrucache.strategy.LFUEvictionPolicy;
import com.lrucache.strategy.LinkedHashMapLRUEvictionPolicy;
import com.lrucache.strategy.LRUEvictionPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class LruCacheDemo {

    public static void main(String[] args) {
        runLRU();
        System.out.println();
        runLFU();
        System.out.println();
        runFIFO();
        System.out.println();
        runLinkedHashMapLRU();
        System.out.println();
        runUpdateOnFullCache();
        System.out.println();
        runTTL();
    }

    private static void runLRU() {
        System.out.println("=== LRU Cache (capacity 3) ===");
        Cache<Integer, String> cache = new Cache<>(3, new LRUEvictionPolicy<>());

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        System.out.println("After puts {1,2,3} -> size=" + cache.size());

        // Touch key 1 so it becomes most recently used
        cache.get(1).ifPresent(v -> System.out.println("get(1) = " + v));

        // Inserting 4 should now evict key 2 (the least recently used)
        cache.put(4, "four");
        System.out.println("After put(4) -> get(2) present? " + cache.get(2).isPresent());
        System.out.println("get(1) = " + cache.get(1).orElse("EVICTED"));
        System.out.println("get(3) = " + cache.get(3).orElse("EVICTED"));
        System.out.println("get(4) = " + cache.get(4).orElse("EVICTED"));
    }

    private static void runLFU() {
        System.out.println("=== LFU Cache (capacity 3) ===");
        Cache<Integer, String> cache = new Cache<>(3, new LFUEvictionPolicy<>());

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // Bump frequencies: key 1 -> 3 hits, key 2 -> 2 hits, key 3 -> 1 hit (the put)
        cache.get(1);
        cache.get(1);
        cache.get(2);

        // Key 3 has the lowest frequency, so it gets evicted when we add a new key
        cache.put(4, "four");
        System.out.println("After put(4) -> get(3) present? " + cache.get(3).isPresent());
        System.out.println("get(1) = " + cache.get(1).orElse("EVICTED"));
        System.out.println("get(2) = " + cache.get(2).orElse("EVICTED"));
        System.out.println("get(4) = " + cache.get(4).orElse("EVICTED"));
    }

    private static void runFIFO() {
        System.out.println("=== FIFO Cache (capacity 3) ===");
        Cache<Integer, String> cache = new Cache<>(3, new FIFOEvictionPolicy<>());

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Hammer key 1. Under LRU this would save it; under FIFO it changes
        // nothing at all -- reads never reorder arrivals.
        cache.get(1);
        cache.get(1);
        cache.get(1);

        cache.put(4, "four");
        System.out.println("Key 1 was read 3x but arrived first -> get(1) present? " + cache.get(1).isPresent());
        System.out.println("get(2) = " + cache.get(2).orElse("EVICTED"));
        System.out.println("get(3) = " + cache.get(3).orElse("EVICTED"));
        System.out.println("get(4) = " + cache.get(4).orElse("EVICTED"));
    }

    /**
     * Same scenario as runLRU(), same output -- but the policy is five lines
     * of LinkedHashMap instead of a hand-rolled DLL. Ship this; don't lead
     * with it in an interview (see the class javadoc for why).
     */
    private static void runLinkedHashMapLRU() {
        System.out.println("=== LRU via LinkedHashMap(accessOrder=true) (capacity 3) ===");
        Cache<Integer, String> cache = new Cache<>(3, new LinkedHashMapLRUEvictionPolicy<>());

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.get(1);                       // key 1 becomes MRU

        cache.put(4, "four");               // evicts key 2, exactly like runLRU()
        System.out.println("After put(4) -> get(2) present? " + cache.get(2).isPresent());
        System.out.println("get(1) = " + cache.get(1).orElse("EVICTED"));
        System.out.println("get(3) = " + cache.get(3).orElse("EVICTED"));
        System.out.println("get(4) = " + cache.get(4).orElse("EVICTED"));
        System.out.println("(identical to the hand-rolled LRU above)");
    }

    /**
     * Guards the classic LRU implementation bug: writing an existing key into
     * a FULL cache is an UPDATE, not an insert. It must not evict anybody, and
     * it must not register a second node for the same key in the policy.
     */
    private static void runUpdateOnFullCache() {
        System.out.println("=== Update on a full cache (LRU, capacity 3) ===");
        Cache<Integer, String> cache = new Cache<>(3, new LRUEvictionPolicy<>());

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Update key 2 while the cache is at capacity.
        cache.put(2, "TWO-updated");
        System.out.println("size after update (must stay 3) = " + cache.size());
        System.out.println("get(1) survived the update? " + cache.get(1).isPresent());
        System.out.println("get(2) = " + cache.get(2).orElse("EVICTED"));

        // Recency after the update + the get(1) above: 2 is MRU, then 1, so 3
        // is the LRU and must be the one to go.
        cache.put(5, "five");
        System.out.println("After put(5) -> get(3) evicted? " + !cache.get(3).isPresent());
        System.out.println("get(2) still here (was MRU)? " + cache.get(2).isPresent());
    }

    private static void runTTL() {
        System.out.println("=== TTL (LRU, capacity 3, default TTL = 5s) ===");

        // A hand-cranked clock instead of Thread.sleep: TTL behaviour becomes
        // deterministic and instant. This is exactly what a unit test does.
        SteppableClock clock = new SteppableClock(Instant.parse("2026-01-01T00:00:00Z"));
        Cache<Integer, String> cache =
                new Cache<>(3, new LRUEvictionPolicy<>(), Duration.ofSeconds(5), clock);

        cache.put(1, "one");
        cache.put(2, "two", Duration.ofSeconds(30));  // per-entry TTL beats the default
        cache.put(3, "three", null);                  // null TTL = never expires

        clock.advance(Duration.ofSeconds(6));
        System.out.println("t+6s -> get(1) [5s ttl] = " + cache.get(1).orElse("EXPIRED"));
        System.out.println("t+6s -> get(2) [30s ttl] = " + cache.get(2).orElse("EXPIRED"));
        System.out.println("t+6s -> get(3) [no ttl]  = " + cache.get(3).orElse("EXPIRED"));
        System.out.println("size after lazy expiry of key 1 = " + cache.size());

        // Rewriting a key restarts its clock.
        cache.put(4, "four");
        clock.advance(Duration.ofSeconds(4));
        cache.put(4, "four-refreshed");
        clock.advance(Duration.ofSeconds(4));
        System.out.println("t+14s -> get(4) refreshed at t+10s = " + cache.get(4).orElse("EXPIRED"));

        // A full cache reclaims dead entries before evicting a live one.
        clock.advance(Duration.ofSeconds(60));   // everything with a TTL is now stale
        System.out.println("purgeExpired() removed = " + cache.purgeExpired());
        System.out.println("survivors = size " + cache.size() + " (only key 3, the no-TTL entry)");
    }

    /**
     * Minimal manually-advanced Clock. Demonstrates why Cache takes a Clock
     * rather than calling Instant.now(): TTL becomes testable without sleeping.
     */
    private static final class SteppableClock extends Clock {
        private Instant now;

        SteppableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
