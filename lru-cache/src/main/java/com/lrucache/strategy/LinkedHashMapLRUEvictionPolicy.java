package com.lrucache.strategy;

import java.util.LinkedHashMap;

/**
 * LRU in five lines of real logic, by letting java.util do the work.
 *
 * READ THIS BEFORE USING IT IN AN INTERVIEW
 * -----------------------------------------
 * This class is behaviourally identical to LRUEvictionPolicy -- same O(1)
 * complexity, same eviction order, fewer bugs, less code. In production this
 * is the version you ship.
 *
 * In an interview it is the version that gets you rejected, because the
 * question "implement an LRU cache" is not asking for a cache. It is asking
 * whether you can build the doubly-linked-list-plus-hashmap structure that
 * makes O(1) recency possible. Handing back `new LinkedHashMap<>(16, 0.75f,
 * true)` answers a question nobody asked and shows none of the reasoning the
 * interviewer is grading: why a DLL and not a singly linked list, why
 * sentinels, why a key->node map. The library is doing the exact thing you
 * were asked to demonstrate -- LinkedHashMap IS a HashMap whose entries are
 * threaded onto a doubly linked list.
 *
 * The right way to use it: build LRUEvictionPolicy by hand first, then say
 * "in production I'd reach for LinkedHashMap with accessOrder=true, which is
 * this same structure already implemented in the JDK." That reads as range.
 * Leading with it reads as avoidance.
 *
 * How it works: the third constructor argument, accessOrder=true, is the
 * whole trick. It flips LinkedHashMap from insertion-order to access-order,
 * so every get() and put() re-threads that entry to the END of the internal
 * linked list. The head of the iteration order is therefore always the
 * least-recently-used key -- exactly what selectEvictionCandidate needs.
 *
 * Note the value type is Boolean and never read: this policy only tracks
 * keys, so the map is being used as an ordered set. The real values live in
 * Cache's own map.
 */
public class LinkedHashMapLRUEvictionPolicy<K> implements EvictionPolicy<K> {

    // accessOrder = true is the entire policy. With the default (false) this
    // class would silently behave as FIFO instead of LRU -- the two differ by
    // one boolean, which is a nice thing to be able to point out.
    private final LinkedHashMap<K, Boolean> order = new LinkedHashMap<>(16, 0.75f, true);

    @Override
    public void keyAdded(K key) {
        order.put(key, Boolean.TRUE);
    }

    /**
     * The lookup is not pointless -- on an access-ordered LinkedHashMap, get()
     * has the side effect of moving the entry to the most-recently-used end.
     * That side effect IS the promotion; the returned value is discarded.
     * (containsKey would NOT work here: it deliberately does not reorder.)
     */
    @Override
    public void keyAccessed(K key) {
        order.get(key);
    }

    @Override
    public void keyRemoved(K key) {
        order.remove(key);
    }

    /**
     * Iteration on an access-ordered LinkedHashMap runs least-recently-used
     * first, so the first key is the victim. O(1) -- the iterator jumps
     * straight to the list head, it does not scan.
     */
    @Override
    public K selectEvictionCandidate() {
        if (order.isEmpty()) {
            return null;
        }
        return order.keySet().iterator().next();
    }
}
