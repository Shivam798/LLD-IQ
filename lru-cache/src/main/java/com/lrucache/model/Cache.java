package com.lrucache.model;

import com.lrucache.strategy.EvictionPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic, fixed-capacity in-memory cache. Eviction order is delegated to an
 * EvictionPolicy strategy (LRU, LFU, or any future policy). The cache owns
 * the K -> V storage; the policy owns the access bookkeeping.
 *
 * All public methods are synchronized to keep the cache and its policy in
 * lockstep under concurrent access. Reads and writes both mutate the policy
 * (touching access order or frequency), so even get() needs the lock.
 */
public class Cache<K, V> {

    private final int capacity;
    private final Map<K, V> data;
    private final EvictionPolicy<K> policy;

    public Cache(int capacity, EvictionPolicy<K> policy) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (policy == null) {
            throw new IllegalArgumentException("EvictionPolicy is required");
        }
        this.capacity = capacity;
        this.policy = policy;
        this.data = new HashMap<>();
    }

    public synchronized Optional<V> get(K key) {
        V value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        policy.keyAccessed(key);
        return Optional.of(value);
    }

    public synchronized void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        if (data.containsKey(key)) {
            data.put(key, value);
            policy.keyAccessed(key);
            return;
        }

        if (data.size() == capacity) {
            K victim = policy.selectEvictionCandidate();
            if (victim != null) {
                data.remove(victim);
                policy.keyRemoved(victim);
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
}
