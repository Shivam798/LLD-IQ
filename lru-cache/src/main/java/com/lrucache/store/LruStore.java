package com.lrucache.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Least Recently Used, single-map edition.
 *
 *   table:  K -> Node          (one hash lookup gets you the value AND the position)
 *   list:   head <-> MRU <-> ... <-> LRU <-> tail
 *
 * The node carries the value, so get() hashes once. Compare with
 * {@link com.lrucache.strategy.LRUEvictionPolicy}, which hashes twice --
 * once in Cache.data for the value, once in the policy's nodeMap for the node.
 *
 * Why a doubly-linked list and not an ArrayDeque or a LinkedList:
 *   - we must unlink a node from the MIDDLE in O(1) on every get(). A singly
 *     linked list can't (no back-pointer), and java.util.LinkedList can't
 *     either, because its remove(Object) is O(n) -- it has the pointers but
 *     gives you no handle on the node.
 *   - the handle is the whole trick: table.get(key) hands us the exact node,
 *     and prev/next let us splice it out without touching anything else.
 *
 * Head and tail are sentinels holding no data. They exist so "the node after
 * head" and "the node before tail" are always non-null, which deletes every
 * null check from addToHead and unlink. Two wasted objects for branchless
 * pointer surgery is a trade worth making once per cache.
 *
 * Thread safety: all mutating entry points are synchronized. get() mutates
 * (it reorders the list), so it takes the lock too -- a read here is a write.
 */
public class LruStore<K, V> implements CacheStore<K, V> {

    /**
     * private + static + nested:
     *   private : nothing outside this class should touch list internals
     *   static  : no implicit reference to the enclosing LruStore, so each
     *             node is 2 words lighter -- matters at cache scale
     *   nested  : it has exactly one consumer, so it lives next to it
     */
    private static final class Node<K, V> {
        final K key;     // needed on eviction: we hold the node, we must find the map entry
        V value;         // NOT final -- put() on an existing key overwrites in place
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> table = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    public LruStore(int capacity) {
        if (capacity <= 0) {
            // Guard the degenerate case explicitly. A capacity of 0 would make
            // put() try to evict from an empty list and walk into the head
            // sentinel's null prev pointer -- the classic NPE in a LeetCode
            // solution that was never asked to handle it.
            throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
        }
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = table.get(key);
        if (node == null) {
            return Optional.empty();
        }
        // A hit is a use. Promote to the MRU end, or the list would never
        // reorder and every eviction would be FIFO by accident.
        moveToHead(node);
        return Optional.of(node.value);
    }

    @Override
    public synchronized Optional<K> put(K key, V value) {
        requireNonNull(key, value);

        Node<K, V> existing = table.get(key);
        if (existing != null) {
            // Overwrite is an update AND a use -- but never an insert. Nothing
            // is evicted, because size didn't change. Getting this wrong is the
            // bug that evicts a live entry on a repeated put of the same key.
            existing.value = value;
            moveToHead(existing);
            return Optional.empty();
        }

        Optional<K> evicted = Optional.empty();
        if (table.size() >= capacity) {
            // >= not ==. Equality is correct only as long as nothing else can
            // overshoot capacity; >= survives the day someone adds a bulk path.
            Node<K, V> victim = tail.prev;   // the LRU end, never the sentinel:
            unlink(victim);                  // we only get here when size >= 1
            table.remove(victim.key);        // <- this is why Node holds its key
            evicted = Optional.of(victim.key);
        }

        Node<K, V> fresh = new Node<>(key, value);
        table.put(key, fresh);
        addToHead(fresh);
        return evicted;
    }

    @Override
    public synchronized Optional<V> remove(K key) {
        Node<K, V> node = table.remove(key);
        if (node == null) {
            return Optional.empty();
        }
        unlink(node);
        return Optional.of(node.value);
    }

    @Override
    public synchronized int size() {
        return table.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    /**
     * Walk from the tail backwards: the LRU end leaves first.
     */
    @Override
    public synchronized List<K> evictionOrder() {
        List<K> order = new ArrayList<>(table.size());
        for (Node<K, V> cur = tail.prev; cur != head; cur = cur.prev) {
            order.add(cur.key);
        }
        return order;
    }

    // ---- list surgery -------------------------------------------------

    private void moveToHead(Node<K, V> node) {
        unlink(node);
        addToHead(node);
    }

    /**
     * Splice node between head and head.next -- the MRU position.
     *
     *   before:  head <-> X <-> ...
     *   after:   head <-> node <-> X <-> ...
     */
    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * Detach node. O(1) wherever it sits, because it has a back-pointer.
     *
     *   before:  A <-> node <-> B
     *   after:   A <-> B
     *
     * prev/next are nulled so an evicted node can't be used to walk back into
     * the live list. Harmless on the moveToHead path (addToHead overwrites both
     * immediately), but on the eviction path it stops a detached node from
     * pinning the rest of the list alive for the GC.
     */
    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    private void requireNonNull(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
    }

    @Override
    public String toString() {
        return "LruStore" + evictionOrder() + " (index 0 evicts next)";
    }
}
