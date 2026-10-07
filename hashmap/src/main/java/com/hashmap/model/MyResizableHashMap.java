package com.hashmap.model;

/**
 * The LLD-round version: MyHashMap with generic keys and a table that grows.
 *
 * Same buckets, same chains, same put / get / remove. Only three things are new:
 *   1. <K, V>: the bucket comes from key.hashCode(), a match is equals().
 *   2. A size counter.
 *   3. Once size > capacity * 0.75, resize() doubles the table, so chains
 *      stay short no matter how many entries arrive.
 *
 * get returns null for "absent". Null keys are not supported.
 */
public class MyResizableHashMap<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final int INITIAL_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;   // lower = emptier buckets, higher = longer chains

    @SuppressWarnings("unchecked")                    // Java can't create a generic array directly
    private Node<K, V>[] table = new Node[INITIAL_CAPACITY];
    private int size;

    public void put(K key, V value) {
        int i = index(key);

        for (Node<K, V> cur = table[i]; cur != null; cur = cur.next) {
            if (cur.key.equals(key)) {        // already present -> overwrite
                cur.value = value;
                return;
            }
        }

        table[i] = new Node<>(key, value, table[i]); // prepend as the new head
        size++;

        if (size > table.length * LOAD_FACTOR) {     // 13th entry into 16 buckets -> 32 buckets
            resize();
        }
    }

    public V get(K key) {
        for (Node<K, V> cur = table[index(key)]; cur != null; cur = cur.next) {
            if (cur.key.equals(key)) return cur.value;
        }
        return null;
    }

    public void remove(K key) {
        int i = index(key);
        Node<K, V> dummy = new Node<>(null, null, table[i]); // same sentinel trick as MyHashMap

        for (Node<K, V> prev = dummy; prev.next != null; prev = prev.next) {
            if (prev.next.key.equals(key)) {
                prev.next = prev.next.next;
                size--;
                break;
            }
        }

        table[i] = dummy.next;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return table.length;
    }

    /**
     * Double the table and put every entry back in. Every entry has to move,
     * because index() depends on table.length. O(n), but it happens rarely
     * enough that put is still O(1) amortized.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] old = table;
        table = new Node[old.length * 2];
        size = 0;                             // put() below counts them back up

        for (Node<K, V> head : old) {
            for (Node<K, V> cur = head; cur != null; cur = cur.next) {
                put(cur.key, cur.value);
            }
        }
    }

    private int index(K key) {
        return Math.abs(key.hashCode() % table.length); // hashCode() can be negative
    }
}
