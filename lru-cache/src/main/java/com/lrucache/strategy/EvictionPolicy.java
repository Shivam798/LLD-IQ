package com.lrucache.strategy;

/**
 * Strategy interface for selecting which key the cache should evict when it
 * exceeds capacity. The cache owns the key-to-value storage; the policy owns
 * the access-order bookkeeping.
 *
 * The four hooks let the cache notify the policy of every observable event:
 *  - keyAdded      : a brand new key was inserted
 *  - keyAccessed   : an existing key was read or its value was updated
 *  - keyRemoved    : a key was dropped (manual remove or eviction)
 *  - selectEvictionCandidate : returns which key the policy wants to drop next
 *
 * All four operations are expected to run in O(1) for any production-grade
 * policy (LRU, LFU, FIFO, MRU, etc.).
 */
public interface EvictionPolicy<K> {

    void keyAdded(K key);

    void keyAccessed(K key);

    void keyRemoved(K key);

    K selectEvictionCandidate();
}
