package com.lrucache.strategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Least Recently Used eviction.
 *
 * Maintains a doubly linked list of keys ordered by recency, plus a HashMap
 * from key to its DLL node. Every hook is O(1):
 *
 *   head <-> [MRU] <-> ... <-> [LRU] <-> tail
 *
 *   keyAdded                 : new node added right after head
 *   keyAccessed              : detach existing node, move right after head
 *   keyRemoved               : detach node, drop from map
 *   selectEvictionCandidate  : return tail.prev.key (the LRU end)
 *
 * Head and tail are sentinel nodes -- they eliminate null checks at the
 * boundaries of the list.
 */
public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {

    /**
     * Doubly-linked list node. Private + static + nested because:
     *   - private  : only the policy needs it; no other class should touch DLL guts
     *   - static   : doesn't need a reference to the enclosing instance (saves memory)
     *   - nested   : co-located with its only consumer so the file reads top-to-bottom
     */
    private static final class Node<K> {
        final K key;
        Node<K> prev;
        Node<K> next;

        Node(K key) {
            this.key = key;
        }
    }

    // key -> node. Lets us find any key's DLL node in O(1) so keyAccessed
    // can unlink without scanning the list.
    private final Map<K, Node<K>> nodeMap = new HashMap<>();

    // Sentinel nodes. They hold no real data (key = null) and only exist so
    // that "the node after head" and "the node before tail" are always valid
    // references -- never null. This removes every null check from addToHead
    // and unlink. The two extra allocations are a tiny price for branchless,
    // uniform pointer surgery on every operation.
    private final Node<K> head = new Node<>(null);
    private final Node<K> tail = new Node<>(null);

    public LRUEvictionPolicy() {
        // Wire the sentinels together so the list starts in a valid "empty"
        // state: head <-> tail. Real nodes will be spliced in between them.
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Brand new key: create a node, register it in the map, splice it right
     * after head. "Right after head" = most-recently-used position.
     */
    @Override
    public void keyAdded(K key) {
        Node<K> node = new Node<>(key);
        nodeMap.put(key, node);
        addToHead(node);
    }

    /**
     * Existing key was read or its value was updated. Promote it back to
     * the MRU position: unlink from wherever it currently is, then splice
     * right after head.
     *
     * The unlink + addToHead combo is the heart of LRU. It's O(1) only
     * because (a) nodeMap gives us the node directly, and (b) the DLL gives
     * us back-pointers so unlink doesn't need to walk the list.
     */
    @Override
    public void keyAccessed(K key) {
        Node<K> node = nodeMap.get(key);
        if (node == null) {
            // Caller asked us to track an access on a key we don't know
            // about. Tolerate it silently -- the Cache layer is the source
            // of truth and may have already evicted this key.
            return;
        }
        unlink(node);
        addToHead(node);
    }

    /**
     * Explicit removal (manual cache.remove() or eviction). Drop from both
     * structures so the policy and the cache stay in lockstep.
     */
    @Override
    public void keyRemoved(K key) {
        Node<K> node = nodeMap.remove(key);
        if (node != null) {
            unlink(node);
        }
    }

    /**
     * "Who should we evict?" The least-recently-used key sits at the tail
     * end of the list -- specifically, at tail.prev. We don't remove it
     * here; we just report the candidate. The Cache will call keyRemoved
     * after it deletes the value from its HashMap.
     */
    @Override
    public K selectEvictionCandidate() {
        // tail.prev == head means the list is empty (only sentinels).
        // Nothing to evict.
        if (tail.prev == head) {
            return null;
        }
        return tail.prev.key;
    }

    /**
     * Splice `node` between head and head.next so it becomes the new MRU.
     * Four pointer assignments, no branches, no nulls thanks to sentinels.
     *
     *   Before:  head <-> X <-> ...
     *   After:   head <-> node <-> X <-> ...
     */
    private void addToHead(Node<K> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * Remove `node` from the list. Because every node has both prev and
     * next pointers, this is O(1) regardless of where the node sits.
     *
     *   Before:  A <-> node <-> B
     *   After:   A <-> B          (node is now detached)
     *
     * We null out node.prev/next defensively so a stale reference can't be
     * used to walk into the live list by mistake.
     */
    private void unlink(Node<K> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }
}
