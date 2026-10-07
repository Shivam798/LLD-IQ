package com.hashmap;

import com.hashmap.model.MyConcurrentHashMap;
import com.hashmap.model.MyHashMap;
import com.hashmap.model.MyResizableHashMap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Proof for every claim the README makes. Section 5 reproduces the race rather than describing it. */
public class HashMapDemo {

    private static final int BUCKETS = 769;

    public static void main(String[] args) throws Exception {
        theQuestionAsAsked();
        collisionsShareABucket();
        overwriteDoesNotDuplicate();
        removeFromAnywhereInTheChain();
        theRace();
        putIfAbsentIsAtomic();
        itGrows();
    }

    // 1 -------------------------------------------------------------------

    private static void theQuestionAsAsked() {
        header("1. put / get / remove");

        MyHashMap map = new MyHashMap();
        map.put(1, 1);
        map.put(2, 2);
        System.out.println("get(1) = " + map.get(1) + "   expected 1");
        System.out.println("get(3) = " + map.get(3) + "  expected -1 (absent)");
        map.put(2, 1);
        System.out.println("get(2) = " + map.get(2) + "   expected 1 (overwritten)");
        map.remove(2);
        System.out.println("get(2) = " + map.get(2) + "  expected -1 (removed)");
        System.out.println();
        System.out.println("-1 is safe as \"absent\" only because the problem pins 0 <= value <= 10^6.");
    }

    // 2 -------------------------------------------------------------------

    private static void collisionsShareABucket() {
        header("2. Collisions -- one bucket, one chain");

        MyHashMap map = new MyHashMap();
        int[] keys = {1, 770, 1539, 2308};   // all congruent to 1 mod 769
        for (int k : keys) {
            map.put(k, k * 10);
        }
        for (int k : keys) {
            System.out.printf("key %-5d -> bucket %-3d value %d%n", k, k % BUCKETS, map.get(k));
        }
        System.out.println("4 keys, 1 bucket, nothing lost -- they chain. Lookups there are O(4).");
    }

    // 3 -------------------------------------------------------------------

    private static void overwriteDoesNotDuplicate() {
        header("3. put on an existing key overwrites");

        MyHashMap map = new MyHashMap();
        map.put(7, 100);
        map.put(7, 200);
        map.put(7, 300);
        System.out.println("get(7) = " + map.get(7) + " expected 300 (the last write)");
        map.remove(7);
        System.out.println("after one remove, get(7) = " + map.get(7) + " expected -1");
        System.out.println("Skip the scan in put and you get 3 nodes for key 7 -- one remove would");
        System.out.println("uncover a stale duplicate instead of deleting the key.");
    }

    // 4 -------------------------------------------------------------------

    private static void removeFromAnywhereInTheChain() {
        header("4. Remove -- head, middle, tail, and absent");

        int[] keys = {1, 770, 1539};   // one bucket; put prepends, so the chain is 1539 -> 770 -> 1
        for (int target : new int[]{1539, 770, 1, 9999}) {
            MyHashMap map = new MyHashMap();
            for (int k : keys) {
                map.put(k, k);
            }
            map.remove(target);
            StringBuilder sb = new StringBuilder();
            for (int k : keys) {
                sb.append(k).append('=').append(map.get(k)).append("  ");
            }
            System.out.printf("remove(%-4d) -> %s%n", target, sb);
        }
        System.out.println("The dummy node dissolves both awkward cases: removing the head, and an");
        System.out.println("empty bucket where dummy.next is null and the loop never runs.");
    }

    // 5 -------------------------------------------------------------------

    private static void theRace() throws Exception {
        header("5. Two threads, one map");

        int threads = 8;
        int perThread = 2_000;
        int expected = threads * perThread;
        System.out.printf("%d threads x %d distinct keys = %d expected entries%n%n", threads, perThread, expected);

        MyHashMap unsafe = new MyHashMap();
        run(threads, perThread, (t, k) -> unsafe.put(t * perThread + k, t * perThread + k));

        MyConcurrentHashMap safe = new MyConcurrentHashMap();
        run(threads, perThread, (t, k) -> safe.put(t * perThread + k, t * perThread + k));

        int unsafeFound = 0, safeFound = 0;
        for (int k = 0; k < expected; k++) {
            if (unsafe.get(k) == k) unsafeFound++;
            if (safe.get(k) == k) safeFound++;
        }

        System.out.printf("MyHashMap            found %-6d LOST %d%n", unsafeFound, expected - unsafeFound);
        System.out.printf("MyConcurrentHashMap  found %-6d LOST %d   (size() = %d)%n",
                safeFound, expected - safeFound, safe.size());
        System.out.println();
        System.out.println("MyHashMap loses entries because two threads prepending to the same bucket");
        System.out.println("both read the old head, and the second write erases the first's node.");
        System.out.println("Numbers vary per run -- that is what a data race is.");
    }

    // 6 -------------------------------------------------------------------

    private static void putIfAbsentIsAtomic() throws Exception {
        header("6. putIfAbsent -- a safe map is not a safe caller");

        int threads = 16;
        MyConcurrentHashMap map = new MyConcurrentHashMap();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicCounter winners = new AtomicCounter();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            final int id = t;
            pool.execute(() -> {
                await(start);
                if (map.putIfAbsent(42, id) == -1) winners.increment();
                done.countDown();
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();

        System.out.println(threads + " threads called putIfAbsent(42, ...)");
        System.out.println("threads that inserted = " + winners.get() + "   expected exactly 1");
        System.out.println();
        System.out.println("Written by the caller instead, on the SAME thread-safe map:");
        System.out.println("    if (map.get(k) == -1) map.put(k, v);");
        System.out.println("both threads pass the check before either writes. Each call is atomic;");
        System.out.println("the pair is not.");
    }

    // 7 -------------------------------------------------------------------

    private static void itGrows() {
        header("7. MyResizableHashMap -- generic, and the table grows");

        MyResizableHashMap<Integer, Integer> map = new MyResizableHashMap<>();
        System.out.println("starts at capacity " + map.capacity() + ", load factor 0.75 -> resizes past 12 entries");
        int lastCapacity = map.capacity();
        for (int k = 1; k <= 100_000; k++) {
            map.put(k, k * 10);
            if (map.capacity() != lastCapacity) {
                if (map.capacity() <= 256) {
                    System.out.printf("  put #%-6d size %-6d capacity %d -> %d%n", k, map.size(), lastCapacity, map.capacity());
                }
                lastCapacity = map.capacity();
            }
        }
        System.out.println("  ... and so on");

        int found = 0;
        for (int k = 1; k <= 100_000; k++) {
            if (map.get(k) == k * 10) found++;
        }
        System.out.printf("after 100,000 puts: size %d, capacity %d, found %d of 100000 after every resize%n",
                map.size(), map.capacity(), found);
        System.out.printf("entries per bucket = %.2f -- never above 0.75, so chains stay O(1)%n",
                (double) map.size() / map.capacity());

        System.out.println();
        map.put(-7, 70);
        System.out.println("negative key: get(-7) = " + map.get(-7) + "   (Math.abs keeps the index >= 0)");

        MyResizableHashMap<String, String> names = new MyResizableHashMap<>();
        names.put("alice", "admin");
        names.put("alice", "owner");
        System.out.println("String key: get(\"alice\") = " + names.get("alice") + "   (overwritten, size " + names.size() + ")");
        System.out.println("missing:    get(\"bob\")   = " + names.get("bob") + "    (null, not -1)");
        names.remove("alice");
        System.out.println("removed:    get(\"alice\") = " + names.get("alice") + ", size " + names.size());
    }

    // helpers --------------------------------------------------------------

    private interface Put {
        void apply(int threadId, int key);
    }

    /** Starts every thread at the same instant, to maximise overlap. */
    private static void run(int threads, int perThread, Put put) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            final int id = t;
            pool.execute(() -> {
                await(start);
                for (int k = 0; k < perThread; k++) {
                    put.apply(id, k);
                }
                done.countDown();
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class AtomicCounter {
        private final java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger();
        void increment() { n.incrementAndGet(); }
        int get() { return n.get(); }
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println(title);
        System.out.println("=".repeat(70));
    }
}
