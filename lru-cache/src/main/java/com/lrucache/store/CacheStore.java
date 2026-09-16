package com.lrucache.store;

import java.util.List;
import java.util.Optional;

/**
 * A fixed-capacity cache whose eviction rule IS the implementation.
 *
 * This is the second of the two designs in this module, and it exists to make
 * a trade-off visible:
 *
 *   com.lrucache.strategy.EvictionPolicy<K>  -- the policy tracks keys only.
 *       The Cache owns key -> value, the policy owns key -> recency. Two maps,
 *       two hash lookups per get(). Clean separation, slower hot path.
 *
 *   com.lrucache.store.CacheStore<K, V>      -- this one. The store owns the
 *       node, and the node carries the value. ONE map, ONE hash lookup per
 *       get(). The eviction rule and the storage live together.
 *
 * Both are pluggable; they just draw the seam in a different place. This one
 * draws it one level higher -- you swap the whole store rather than swapping a
 * policy inside a shared cache. You lose the ability to layer TTL/stats/metrics
 * in one place (see {@link com.lrucache.model.Cache}); you gain a cache that
 * does exactly one hash per operation, which is what a real LRU in front of a
 * database looks like.
 *
 * The interface is deliberately narrow (ISP): four operations a caller needs,
 * plus two inspectors. Nothing here mentions recency, frequency, linked lists
 * or buckets -- that vocabulary belongs to the implementations, which is the
 * test of whether an abstraction is genuinely generic.
 *
 * Contract every implementation must honour:
 *   - capacity is fixed at construction and > 0
 *   - size() never exceeds capacity()
 *   - get, put and remove are O(1) -- no implementation may scan
 *   - get() on a present key counts as a use and may reorder the store
 *   - put() on a present key updates the value and also counts as a use
 *   - null keys and null values are rejected
 */
public interface CacheStore<K, V> {

    /**
     * Look up a key. A hit counts as a USE -- LRU promotes the entry, LFU
     * increments its frequency. That is why this is not a read-only method
     * and why implementations synchronize it.
     *
     * @return the value, or empty if the key is absent
     */
    Optional<V> get(K key);

    /**
     * Insert a new entry or overwrite an existing one. If the store is at
     * capacity and the key is new, exactly one entry is evicted first --
     * which one is the whole point of the implementation.
     *
     * @return the evicted key, or empty if nothing was evicted (store had
     *         room, or the key already existed and was merely updated)
     */
    Optional<K> put(K key, V value);

    /**
     * Explicit removal. Not an eviction -- the caller decided.
     *
     * @return the value that was removed, or empty if the key was absent
     */
    Optional<V> remove(K key);

    int size();

    int capacity();

    /**
     * Keys in the order this store would evict them: index 0 leaves next.
     *
     * Inspection only -- O(n) and not part of the hot path. It exists so the
     * demo (and a test) can assert on ordering without reflecting into the
     * private node graph, which is the difference between a design you can
     * prove correct and one you can only eyeball.
     */
    List<K> evictionOrder();
}
