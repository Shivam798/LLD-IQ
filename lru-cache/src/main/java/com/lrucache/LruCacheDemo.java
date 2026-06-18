package com.lrucache;

import com.lrucache.model.Cache;
import com.lrucache.strategy.LFUEvictionPolicy;
import com.lrucache.strategy.LRUEvictionPolicy;

public class LruCacheDemo {

    public static void main(String[] args) {
        runLRU();
        System.out.println();
        runLFU();
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
}
