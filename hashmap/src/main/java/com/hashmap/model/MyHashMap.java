package com.hashmap.model;

/**
 * LeetCode 706 — Design HashMap.
 *
 * Array of B buckets, each bucket a hand-rolled singly-linked chain.
 * Three methods: put, get, remove.
 */
public class MyHashMap {

    private static class Node {
        int key, value;
        Node next;

        Node(int key, int value, Node next) { // next = old chain head, so insert is O(1) at the front
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final int BUCKETS = 769;   // prime, so key % BUCKETS doesn't cluster on strided keys
    private final Node[] table = new Node[BUCKETS];

    public MyHashMap() {
    }

    public void put(int key, int value) {
        int i = hash(key);

        for (Node cur = table[i]; cur != null; cur = cur.next) {
            if (cur.key == key) {             // already present -> overwrite, don't insert a duplicate
                cur.value = value;
                return;
            }
        }

        table[i] = new Node(key, value, table[i]); // prepend as the new head
    }

    public int get(int key) {
        for (Node cur = table[hash(key)]; cur != null; cur = cur.next) {
            if (cur.key == key) return cur.value;
        }
        return -1;                            // problem-specified "no mapping"
    }

    public void remove(int key) {
        int i = hash(key);
        Node dummy = new Node(-1, -1, table[i]); // sentinel in front: unlinking the head needs no special case

        for (Node prev = dummy; prev.next != null; prev = prev.next) {
            if (prev.next.key == key) {
                prev.next = prev.next.next;   // splice it out
                break;
            }
        }

        table[i] = dummy.next;                // re-read: the removed node may have BEEN the head
    }

    private int hash(int key) {
        return key % BUCKETS;                 // keys are >= 0, so % can't return a negative index
    }
}
