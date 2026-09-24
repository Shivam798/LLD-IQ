package com.lrucache.model;

import com.lrucache.strategy.EvictionPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The interview-sized cache: exactly what you should write on a whiteboard.
 *
 * Fixed capacity, four operations (get / put / remove / size), and one design
 * idea -- the cache owns key -> value storage, the injected EvictionPolicy
 * owns the "who leaves when we're full" decision. Swap LRU for LFU or FIFO
 * without touching a line in here.
 *
 * This is {@link Cache} with every follow-up feature removed. TTL, Clock,
 * ExpiryMode, CacheEntry and purgeExpired all live there instead, because
 * they answer questions an interviewer asks AFTER the base design is on the
 * board ("what if entries go stale", "how would you test that", "should a
 * read extend the life", "who cleans up what nobody reads").
 */
public class SimpleCache<K, V> {

    private final int capacity;
    private final Map<K, V> data = new HashMap<>();
    private final EvictionPolicy<K> policy;

    public SimpleCache(int capacity, EvictionPolicy<K> policy) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (policy == null) {
            throw new IllegalArgumentException("EvictionPolicy is required");
        }
        this.capacity = capacity;
        this.policy = policy;
    }

    /**
     * A read is a use: the policy has to see it, or LRU would never reorder
     * and LFU would never count. This is also why get() takes the lock --
     * as far as concurrency is concerned, a read here is a write.
     */
    public synchronized Optional<V> get(K key) {
        V value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        policy.keyAccessed(key);
        return Optional.of(value);
    }

    public synchronized void put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        if (data.containsKey(key)) {
            // Overwrite is an ACCESS, not a new arrival. Calling keyAdded
            // here would register the key twice inside the policy and corrupt
            // the ordering; FIFO would also wrongly reset its arrival slot.
            // Size is unchanged, so there is nothing to evict either.
            data.put(key, value);
            policy.keyAccessed(key);
            return;
        }

        if (data.size() == capacity) {
            K victim = policy.selectEvictionCandidate();
            if (victim != null) {
                drop(victim);
            }
        }

        data.put(key, value);
        policy.keyAdded(key);
    }

    public synchronized boolean remove(K key) {
        if (data.remove(key) == null) {
            return false;
        }
        policy.keyRemoved(key);
        return true;
    }

    public synchronized int size() {
        return data.size();
    }

    public int capacity() {
        return capacity;
    }

    /**
     * The single place where an entry leaves the cache. Eviction and manual
     * remove both have to touch two structures; funnelling them through one
     * helper means the "data and policy always agree" invariant has exactly
     * one implementation to get right.
     */
    private void drop(K key) {
        data.remove(key);
        policy.keyRemoved(key);
    }
}
