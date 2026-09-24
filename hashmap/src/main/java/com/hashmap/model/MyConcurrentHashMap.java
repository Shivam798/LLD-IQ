package com.hashmap.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * The same map, made thread-safe by changing three things:
 *
 *   1. size            -> AtomicInteger            (size++ is read-add-write)
 *   2. empty bucket    -> compareAndSet, no lock   (loser retries)
 *   3. non-empty bucket-> synchronized (head)      (one lock per bucket, not one for the map)
 *
 * get() does not change at all, and takes no lock.
 * Buckets are fixed at 769 -- no resize, same as MyHashMap.
 */
public class MyConcurrentHashMap {

    private static class Node {
        final int key;                        // final: a key never moves bucket
        volatile int value;                   // volatile: get() reads these without a lock
        volatile Node next;

        Node(int key, int value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final int BUCKETS = 769;

    // CHANGE 1+2 live here: per-bucket compareAndSet, and volatile reads for free.
    private final AtomicReferenceArray<Node> table = new AtomicReferenceArray<>(BUCKETS);
    private final AtomicInteger size = new AtomicInteger();

    public void put(int key, int value) {
        int i = hash(key);

        for (;;) {                                        // retry: a CAS can lose, a head can go stale
            Node head = table.get(i);

            if (head == null) {                           // CHANGE 2: empty bucket, no lock at all
                if (table.compareAndSet(i, null, new Node(key, value, null))) {
                    size.incrementAndGet();
                    return;
                }
                continue;                                 // someone beat us here; look again
            }

            synchronized (head) {                         // CHANGE 3: lock JUST this chain
                if (table.get(i) != head) continue;       // head was removed while we waited -> our lock guards nothing

                Node tail = null;
                for (Node cur = head; cur != null; cur = cur.next) {
                    if (cur.key == key) {
                        cur.value = value;
                        return;
                    }
                    tail = cur;
                }
                tail.next = new Node(key, value, null);   // append, not prepend: prepending would swap out the lock object
                size.incrementAndGet();
                return;
            }
        }
    }

    public int get(int key) {                             // unchanged, and lock-free
        for (Node cur = table.get(hash(key)); cur != null; cur = cur.next) {
            if (cur.key == key) return cur.value;
        }
        return -1;
    }

    public void remove(int key) {
        int i = hash(key);

        for (;;) {
            Node head = table.get(i);
            if (head == null) return;

            synchronized (head) {
                if (table.get(i) != head) continue;

                Node dummy = new Node(-1, -1, head);
                for (Node prev = dummy; prev.next != null; prev = prev.next) {
                    if (prev.next.key == key) {
                        prev.next = prev.next.next;
                        size.decrementAndGet();
                        break;
                    }
                }
                table.set(i, dummy.next);                 // volatile write: readers see the new head at once
                return;
            }
        }
    }

    /**
     * Must live inside the map. A caller cannot build it:
     *     if (map.get(k) == -1) map.put(k, v);   // both threads pass the check, both insert
     * Each call is atomic; the pair is not. Thread safety does not compose.
     */
    public int putIfAbsent(int key, int value) {
        int i = hash(key);

        for (;;) {
            Node head = table.get(i);

            if (head == null) {
                if (table.compareAndSet(i, null, new Node(key, value, null))) {
                    size.incrementAndGet();
                    return -1;                            // we inserted
                }
                continue;
            }

            synchronized (head) {
                if (table.get(i) != head) continue;

                Node tail = null;
                for (Node cur = head; cur != null; cur = cur.next) {
                    if (cur.key == key) return cur.value; // already there
                    tail = cur;
                }
                tail.next = new Node(key, value, null);
                size.incrementAndGet();
                return -1;
            }
        }
    }

    public int size() {
        return size.get();
    }

    private int hash(int key) {
        return key % BUCKETS;
    }
}
