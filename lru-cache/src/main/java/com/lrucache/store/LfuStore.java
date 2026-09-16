package com.lrucache.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Least Frequently Used, O(1) on every operation.
 *
 * The naive LFU keeps a count per key and scans for the minimum on eviction --
 * O(n). A heap gets it to O(log n). This one is O(1), and the trick is to stop
 * searching for the minimum and instead maintain it:
 *
 *   table   : K -> Node                       (one hash: value + freq + position)
 *   buckets : freq -> list of nodes with that freq, MRU at head
 *   minFreq : the smallest freq currently present -- maintained, never searched
 *
 *       buckets[1]:  head <-> D <-> C <-> tail        minFreq = 1
 *       buckets[3]:  head <-> A <-> tail
 *       buckets[9]:  head <-> B <-> tail
 *
 * Eviction is then buckets[minFreq].last() -- the least frequent, and among
 * ties the least recent. LFU needs an LRU tie-break or it can't choose between
 * two keys of equal count, which is why every bucket is itself an ordered list
 * and not a set.
 *
 * minFreq only ever moves in two ways:
 *   - a touch that empties bucket[minFreq] pushes it to minFreq + 1, because
 *     the node that just left landed in exactly that bucket
 *   - an insert resets it to 1, because a brand new node has freq 1 and
 *     nothing can be lower
 * Both are O(1). No scan.
 *
 * NOTE how little this shares with {@link LruStore}: no shared base class, a
 * completely different data structure, a different notion of "next victim".
 * That is the point of {@link CacheStore} -- if the interface only fit a
 * linked list, it would be an LRU interface wearing a generic name.
 *
 * Thread safety: mutating entry points are synchronized. get() bumps a
 * frequency, so it mutates and takes the lock.
 */
public class LfuStore<K, V> implements CacheStore<K, V> {

    private static final class Node<K, V> {
        final K key;
        V value;
        int freq = 1;   // a node is born having been used once -- by the put that created it
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * A sentinel-bounded doubly linked list, one per frequency. Same pointer
     * surgery as LruStore, extracted here because LFU needs many of them --
     * one per distinct frequency -- rather than one for the whole store.
     */
    private static final class NodeList<K, V> {
        private final Node<K, V> head = new Node<>(null, null);
        private final Node<K, V> tail = new Node<>(null, null);

        NodeList() {
            head.next = tail;
            tail.prev = head;
        }

        void addToHead(Node<K, V> node) {
            node.prev = head;
            node.next = head.next;
            head.next.prev = node;
            head.next = node;
        }

        void unlink(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
        }

        /** The least-recently-used node at this frequency -- the tie-break victim. */
        Node<K, V> last() {
            return tail.prev;
        }

        boolean isEmpty() {
            return head.next == tail;
        }

        /** LRU-first, for inspection only. */
        List<K> keysLruFirst() {
            List<K> keys = new ArrayList<>();
            for (Node<K, V> cur = tail.prev; cur != head; cur = cur.prev) {
                keys.add(cur.key);
            }
            return keys;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> table = new HashMap<>();
    private final Map<Integer, NodeList<K, V>> buckets = new HashMap<>();
    private int minFreq = 0;

    public LfuStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
        }
        this.capacity = capacity;
    }

    @Override
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = table.get(key);
        if (node == null) {
            return Optional.empty();
        }
        touch(node);
        return Optional.of(node.value);
    }

    @Override
    public synchronized Optional<K> put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        Node<K, V> existing = table.get(key);
        if (existing != null) {
            // Update + use. Nothing evicted: size is unchanged.
            existing.value = value;
            touch(existing);
            return Optional.empty();
        }

        Optional<K> evicted = Optional.empty();
        if (table.size() >= capacity) {
            evicted = Optional.of(evictLeastFrequent());
        }

        Node<K, V> fresh = new Node<>(key, value);
        table.put(key, fresh);
        bucketFor(1).addToHead(fresh);
        // A new arrival has freq 1, and nothing can be lower, so the minimum
        // is known without looking. This is the line that keeps eviction O(1).
        minFreq = 1;
        return evicted;
    }

    @Override
    public synchronized Optional<V> remove(K key) {
        Node<K, V> node = table.remove(key);
        if (node == null) {
            return Optional.empty();
        }
        detach(node);
        // Explicit removal can empty the minFreq bucket without promoting
        // anything into minFreq + 1, leaving minFreq pointing at a bucket that
        // no longer exists. Today that is unobservable -- a remove drops size
        // below capacity, and the next put therefore inserts (resetting minFreq
        // to 1) instead of evicting -- but that is a two-hop argument about
        // another method, not an invariant. Repairing it here keeps "minFreq is
        // the smallest live frequency" true at all times, so evictLeastFrequent
        // can trust it without knowing what put() does. O(distinct frequencies),
        // off the hot path. LeetCode's LFU has no remove(), so the question
        // never comes up there.
        if (!buckets.containsKey(minFreq)) {
            minFreq = table.isEmpty() ? 0 : Collections.min(buckets.keySet());
        }
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
     * Lowest frequency first; within a frequency, least recently used first.
     * Exactly the order evictLeastFrequent would consume them.
     */
    @Override
    public synchronized List<K> evictionOrder() {
        List<K> order = new ArrayList<>(table.size());
        for (Integer freq : new TreeSet<>(buckets.keySet())) {
            order.addAll(buckets.get(freq).keysLruFirst());
        }
        return order;
    }

    // ---- internals ----------------------------------------------------

    /**
     * Promote a node one frequency up: out of its current bucket, into the
     * next. If that emptied the minimum bucket, the minimum is now exactly
     * one higher -- because the node we just moved is what landed there.
     */
    private void touch(Node<K, V> node) {
        int oldFreq = node.freq;
        NodeList<K, V> bucket = buckets.get(oldFreq);
        bucket.unlink(node);
        if (bucket.isEmpty()) {
            buckets.remove(oldFreq);
            if (minFreq == oldFreq) {
                minFreq = oldFreq + 1;
            }
        }
        node.freq = oldFreq + 1;
        bucketFor(node.freq).addToHead(node);
    }

    /**
     * Drop the least frequent key; ties broken by least recently used.
     * Only called when size >= capacity >= 1, so the bucket is never empty.
     */
    private K evictLeastFrequent() {
        NodeList<K, V> bucket = buckets.get(minFreq);
        Node<K, V> victim = bucket.last();
        table.remove(victim.key);   // <- why Node carries its own key
        bucket.unlink(victim);
        if (bucket.isEmpty()) {
            buckets.remove(minFreq);
        }
        return victim.key;
    }

    /** Unlink a node from whichever bucket it is in, cleaning up an empty bucket. */
    private void detach(Node<K, V> node) {
        NodeList<K, V> bucket = buckets.get(node.freq);
        bucket.unlink(node);
        if (bucket.isEmpty()) {
            buckets.remove(node.freq);
        }
    }

    private NodeList<K, V> bucketFor(int freq) {
        return buckets.computeIfAbsent(freq, f -> new NodeList<>());
    }

    @Override
    public String toString() {
        return "LfuStore" + evictionOrder() + " (index 0 evicts next)";
    }
}
