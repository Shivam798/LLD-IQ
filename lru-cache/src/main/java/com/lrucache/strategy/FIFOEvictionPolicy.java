package com.lrucache.strategy;

import java.util.LinkedHashSet;

/**
 * First In, First Out eviction.
 *
 * The simplest possible policy, and the one that makes the EvictionPolicy
 * contract obvious: FIFO cares only about *when a key arrived*, never about
 * how often or how recently it was used. So:
 *
 *   keyAdded                 : append to the back of the insertion order
 *   keyAccessed              : NO-OP -- this is the entire definition of FIFO
 *   keyRemoved               : drop from the insertion order
 *   selectEvictionCandidate  : return the front (the oldest arrival)
 *
 * The no-op keyAccessed is the interview-grade detail. LRU promotes on every
 * read; FIFO deliberately ignores reads. A key that is hammered a million
 * times still gets evicted the moment it becomes the oldest arrival. That is
 * FIFO's weakness (no scan resistance, no recency awareness) and also its
 * strength (zero bookkeeping on the read path, which is the hot path).
 *
 * Why a LinkedHashSet and not an ArrayDeque?
 *   - ArrayDeque gives O(1) addLast / peekFirst, but removing an *arbitrary*
 *     key (a manual cache.remove) is O(n) -- it has to scan.
 *   - LinkedHashSet is a HashSet backed by a doubly linked list of its
 *     entries. It gives O(1) add, O(1) contains/remove of any key, and
 *     iteration in insertion order, so iterator().next() is the oldest key.
 * Every hook stays O(1).
 */
public class FIFOEvictionPolicy<K> implements EvictionPolicy<K> {

    // Keys in arrival order. Head = oldest arrival = next victim.
    // LinkedHashSet's default constructor uses *insertion* order (not access
    // order), which is exactly the FIFO semantic we want -- and unlike
    // LinkedHashMap's access-order mode, there is no way to accidentally
    // reorder it on a read.
    private final LinkedHashSet<K> insertionOrder = new LinkedHashSet<>();

    /**
     * Brand new key: it joins the back of the queue. It will be evicted only
     * after every key currently ahead of it is gone.
     */
    @Override
    public void keyAdded(K key) {
        insertionOrder.add(key);
    }

    /**
     * Deliberately empty.
     *
     * FIFO is defined by arrival order, so a read -- or even a value update
     * on an existing key -- must NOT change a key's position. Re-inserting
     * here would silently turn this into an LRU policy, because LinkedHashSet
     * moves a re-added key only if it was first removed. Leaving this a no-op
     * is the whole policy.
     */
    @Override
    public void keyAccessed(K key) {
        // no-op by design -- see javadoc
    }

    /**
     * Explicit removal (manual cache.remove() or eviction). O(1) because
     * LinkedHashSet hashes to the node and unlinks it directly.
     */
    @Override
    public void keyRemoved(K key) {
        insertionOrder.remove(key);
    }

    /**
     * "Who should we evict?" The oldest arrival -- the first element in
     * insertion order. We only report the candidate; the Cache deletes the
     * value and then calls keyRemoved, so the two stay in lockstep.
     */
    @Override
    public K selectEvictionCandidate() {
        if (insertionOrder.isEmpty()) {
            return null;
        }
        return insertionOrder.iterator().next();
    }
}
