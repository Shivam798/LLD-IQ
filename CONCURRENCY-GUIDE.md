# Java Concurrency & Thread-Safety — Interview Guide

A quick reference for the thread-safety concepts that show up across the LLD problems in this repo. The moment your code contains `volatile`, `synchronized`, `Atomic*`, or a `Concurrent*` collection, an interviewer will ask **"why that one, and not the others?"** This guide is the answer sheet.

> Companion to [UML-ARROWS-GUIDE.md](UML-ARROWS-GUIDE.md). Where that guide explains class *relationships*, this one explains class *thread safety*.

---

## The One Root Cause: Each CPU Core Has Its Own Cache

Almost every concurrency bug and every tool below traces back to a single hardware fact.

Main memory (RAM) is slow, so each CPU core keeps a small, fast **cache** (L1/L2). When a thread reads a field, its core copies the value into cache and works on that **copy** — it does not keep re-reading RAM.

```
   Core 1                Core 2                Core 3 (writer)
 ┌────────┐            ┌────────┐            ┌────────┐
 │ cache  │            │ cache  │            │ cache  │
 │status= │            │status= │            │status= │
 │PENDING │            │PENDING │            │ SENT   │  ← updated only its own cache
 └────────┘            └────────┘            └────────┘
      \___________________|___________________/
                    ┌───────────┐
                    │  RAM      │  status = ??? (may still be PENDING)
                    └───────────┘
```

If Core 3 writes `SENT` but only to its own cache, Cores 1 & 2 keep reading their stale `PENDING` — possibly **forever**. This is the **visibility problem**. Every tool below is a different way of forcing the cores to agree at the right moment.

Two distinct problems fall out of this, and they need different tools:

| Problem | Question | Fixed by |
|---|---|---|
| **Visibility** | Do other threads *see* my latest write? | `volatile` (and the atomics/locks, which include it) |
| **Atomicity** | Can a multi-step update be interrupted halfway? | `synchronized` (lock) or `Atomic*` (CAS) |

---

## `volatile` — the Visibility Tool

```java
private volatile NotificationStatus status;
```

**Guarantees:**
- A `volatile` **write** is flushed to main memory immediately *and* invalidates other cores' cached copies.
- A `volatile` **read** discards the local cache and re-fetches from main memory.
- No **torn reads** — you always see a clean old value or a clean new value, never garbage.

**So with one writer + many readers:** the moment the writer sets the value, every later read sees it. Reads *before* the write correctly see the old value (the write hadn't happened yet) — that's timing, not staleness.

**What `volatile` does NOT do:** make compound actions atomic.

```java
status = next(status);   // read-modify-write — NOT atomic, volatile won't save you
counter++;               // three steps: read, add, write — two threads can lose an update
```

**Use `volatile` when:** a single field is written by one (or few) threads and read by others, with no read-modify-write — a lifecycle flag, a `running` boolean, a config reference, the DCL singleton instance.

---

## `synchronized` — the Lock (Pessimistic)

```java
synchronized long nextId() {
    return counter++;          // whole method is one critical section
}
```

Only one thread can hold the lock (the object's monitor) at a time; others **block** until it's released. This makes the enclosed steps **atomic** *and* provides visibility (entering/leaving a monitor flushes caches, like `volatile`).

**Use `synchronized` when:**
- You do a **read-modify-write**, or
- You must update **several fields together** as one indivisible unit, or
- The critical section is more than a single variable update.

**Keep the block as small as possible** — everything inside is serialized, so an oversized `synchronized` block kills concurrency.

```java
// Prefer a tight block over a synchronized method when only part needs the lock:
void handle() {
    doExpensiveStuffOutsideLock();
    synchronized (this) {
        sharedState.update();   // only this needs protection
    }
}
```

---

## `Atomic*` (AtomicLong, AtomicReference, …) — Lock-Free (Optimistic)

```java
private static final AtomicLong ID_SEQUENCE = new AtomicLong(1);
long id = ID_SEQUENCE.getAndIncrement();   // unique even under concurrency
```

Same correctness as a `synchronized` counter, but **no lock and no blocking**. Built from two pieces:

1. **`volatile long value`** inside → visibility.
2. **CAS (Compare-And-Swap)** → atomicity without a lock.

### How CAS works

CAS is a single CPU instruction (`LOCK CMPXCHG` on x86) that atomically does:

> "Set `value` to N+1 **only if** `value` still equals N (what I last read). Otherwise fail and tell me."

`getAndIncrement()` loops on it:

```text
do {
    current = value;        // volatile read
    next    = current + 1;
} while (!compareAndSwap(current, next));   // retry if someone beat me
return current;
```

Collision example — no one blocks, the loser just retries:

| Step | Thread A | Thread B |
|---|---|---|
| read | 5 | 5 |
| CAS(5→6) | ✅ succeeds, value=6 | ❌ fails (value is 6, not 5) |
| retry | — | reads 6, CAS(6→7) ✅ |

### `getAndIncrement()` vs `incrementAndGet()`

- `getAndIncrement()` → returns the value **before** bumping. `new AtomicLong(1)` ⇒ first id = **1**, then 2, 3…
- `incrementAndGet()` → bumps **first**, returns the new value. `new AtomicLong(0)` ⇒ first id = **1**.

**Use `Atomic*` when:** you update a **single** variable atomically (counter, flag, single reference). For multiple fields, use a lock.

---

## Lock vs CAS — the Mental Model

| | `synchronized` (lock) | `Atomic*` (CAS) |
|---|---|---|
| Strategy | **Pessimistic** — "lock the door, no one else touches it" | **Optimistic** — "try it; if I lost the race, redo it" |
| Other threads | **Block / wait** | **Retry** (never sleep) |
| Cost | Lock acquire/release; contention = waiting | One hardware instruction; contention = a few retries |
| Scope | Multiple fields / multi-step logic | A single variable |
| Risk | Deadlock if multiple locks ordered badly | Livelock under extreme contention (rare) |

One-liner: **single variable → atomic; coordinated multi-field/multi-step → synchronized.**

---

## Double-Checked Locking (DCL) Singleton

```java
private static volatile NotificationService instance;   // volatile is mandatory

public static NotificationService getInstance() {
    if (instance == null) {                  // 1st check: no lock on the hot path
        synchronized (NotificationService.class) {
            if (instance == null) {          // 2nd check: under the lock
                instance = new NotificationService();
            }
        }
    }
    return instance;
}
```

- **First check** avoids paying for the lock on every call once the instance exists.
- **Lock + second check** ensure exactly one instance is constructed under a race.
- **`volatile` is not optional:** without it, instruction reordering can publish the `instance` reference *before* the constructor finishes, letting another thread see a **half-constructed object**. `volatile` forbids that reordering.

---

## `ReentrantLock` — `synchronized` with extra controls

A lock object that does the same job as `synchronized` (mutual exclusion + visibility) but as an **explicit, more flexible API**.

```java
private final ReentrantLock lock = new ReentrantLock();

void update() {
    lock.lock();
    try {
        sharedState.mutate();
    } finally {
        lock.unlock();   // MUST be in finally — unlike synchronized, it won't auto-release
    }
}
```

- **"Reentrant"** = the thread holding the lock can re-acquire it (e.g. a synchronized method calling another) without deadlocking itself. It counts acquisitions and only frees the lock when the count hits zero. (`synchronized` is reentrant too — the name just makes it explicit.)
- **What it adds over `synchronized`:** `tryLock()` (give up if busy instead of blocking forever), `tryLock(timeout)`, `lockInterruptibly()`, optional **fairness** (longest-waiting thread goes next), and the ability to lock/unlock across different method scopes.
- **Cost:** you must `unlock()` in a `finally` yourself — `synchronized` releases automatically even on exception. So prefer `synchronized` for simple cases; reach for `ReentrantLock` only when you need one of its extra features.

---

## Concurrent Collections

| Collection | Use when | Why |
|---|---|---|
| `CopyOnWriteArrayList` | Reads frequent, writes rare (e.g. observer lists) | Lock-free snapshot reads; copies only on write; no `ConcurrentModificationException` mid-iteration |
| `ConcurrentHashMap` | A map hit by many threads | Lock-striping / CAS — concurrent reads and segmented writes, far better than `synchronizedMap` |
| `BlockingQueue` (e.g. `LinkedBlockingQueue`) | Producer/consumer hand-off | Threads block on empty/full automatically |
| `PriorityBlockingQueue` | Producer/consumer **with ordering** (URGENT before LOW) | Priority-ordered drain; basis for "scheduling, not preemption" |

> **Scheduling vs preemption (a classic trap):** a priority queue makes a high-priority item jump ahead of items still **waiting** — it does **not** interrupt an item already being processed. You almost never preempt an in-flight operation (you can't cleanly pause a network call mid-send).

### `CopyOnWriteArrayList` — how it works internally (quick insights)

You already know `volatile` and locks — here's how COW combines them:

- The backing array is a **`volatile Object[]`**. Readers just grab that reference and iterate it — **no lock, no CAS** on the read path. That `volatile` is the whole reason a reader always sees a complete, consistent array.
- Every **mutation** (`add`/`set`/`remove`) takes a **`ReentrantLock`**, copies the *entire* array, mutates the copy, then reassigns the `volatile` reference in one shot. Old readers keep iterating the **old snapshot** — that's why there's no `ConcurrentModificationException`.
- So it's **lock-free reads, lock-on-write (and copy-the-whole-array on write)** — not CAS-based like `AtomicLong`. The lock here guards the copy-then-swap; the `volatile` publishes the new array to readers.
- **Trade-off:** reads are dirt cheap and never block; writes are O(n) and allocate. Perfect for **read-heavy, write-rare** (observer lists, listener registries) — terrible for write-heavy.
- An iterator is a **frozen snapshot** of the array at the moment you got it — it won't reflect later writes and you can't `remove()` through it.

---

## Rapid-Fire Q&A

**Q: What does `volatile` guarantee?**
Visibility + ordering for a single field: writes flush to main memory, reads come from it, no torn values. Not atomicity of compound actions.

**Q: Why isn't `volatile` enough for `count++`?**
`count++` is read-modify-write (three steps); two threads can interleave and lose an update. `volatile` only orders individual reads/writes. Use `Atomic*` or `synchronized`.

**Q: `AtomicLong` vs `synchronized` counter?**
Both correct. Atomic is lock-free (CAS, never blocks) and cheaper for one variable; `synchronized` is for multi-field/multi-step coordination.

**Q: How is an atomic lock-free internally?**
A `volatile` field (visibility) + CAS, a single CPU instruction that updates only if the value is unchanged since you read it, retrying on conflict.

**Q: Why `volatile` on a DCL singleton?**
To stop reordering from publishing a half-constructed instance reference before the constructor completes.

**Q: Why `CopyOnWriteArrayList` for observers?**
Read-heavy, write-rare: lock-free reads, safe iteration during concurrent writes; the copy cost on the rare write is acceptable.

**Q: How does CPU cache relate to all this?**
It's the root cause. Each core caches fields locally, so without sync one core's write is invisible to others. `volatile`/locks/CAS all force the caches to agree at the right moment (cache coherence).

**Q: Difference between scheduling and preemption for priority?**
Scheduling reorders what's *waiting*; preemption interrupts what's *running*. Real systems do the former (priority queue), not the latter.

### Task Scheduler (cron-like) — see [`task-scheduler/`](task-scheduler/)

**Q: Why a `ReentrantLock` + `Condition` for the dispatcher instead of `Thread.sleep(untilDue)`?**
The dispatcher must sleep until the soonest task is due, but a *newly submitted* task that's due sooner has to wake it early. `Thread.sleep` can't be cancelled cleanly; a timed `condition.await(delay)` can — every queue mutation calls `signalAll()` so the dispatcher recomputes the nearest deadline. This is the `DelayQueue`/`ScheduledThreadPoolExecutor` leader-wait pattern.

**Q: Why `signalAll()` after every `schedule`/`cancel`/reschedule?**
Because the change may have produced a new earliest task (or removed the current one). The dispatcher is waiting on a deadline computed from the *old* head; signalling forces it to re-peek and wait on the correct one.

**Q: Why a separate worker pool — why not run the job on the dispatcher thread?**
A single slow job would block every future tick. The dispatcher only *decides when* and hands the task to an `ExecutorService` that *does the work* concurrently. Deciding-when is decoupled from doing-the-work.

**Q: The `Task` fields are `volatile` but not atomic — is that a race?**
No. `status`/`nextExecutionTime`/`runCount` are written by a worker and read by the dispatcher, so they need `volatile` for visibility — but they're only ever *mutated* while the scheduler holds its lock, so there's no read-modify-write interleaving. `volatile` for cross-thread visibility, the lock for the compound update.

**Q: Why must you never change a task's fire time while it's in the `PriorityQueue`?**
A binary heap's ordering invariant breaks if a key changes underneath it — the element ends up in the wrong position and `poll()` returns the wrong minimum. So `nextExecutionTime` is only mutated while the task is *out* of the heap (before first `add`, or after `poll` and before re-`add`).

**Q: Why `PriorityQueue` guarded by a lock instead of `PriorityBlockingQueue`?**
`PriorityBlockingQueue` gives thread-safe `put`/`take`, but the dispatcher needs a *timed, signal-interruptible* wait tied to the head's deadline plus atomic "peek-then-decide-then-maybe-poll". Owning one explicit lock + condition over a plain heap expresses that precisely; the blocking queue would still need external coordination for "wake when a sooner task arrives".

---

## Cheat Sheet — Which Tool?

```
Need to share a single field, written-by-few / read-by-many, no read-modify-write?
    └─> volatile

Need an atomic counter / flag / single reference updated by many threads?
    └─> AtomicLong / AtomicInteger / AtomicReference   (lock-free CAS)

Need to update several fields together, or a multi-step operation, atomically?
    └─> synchronized (smallest block possible)  or a Lock

Need a shared list that's read far more than written?
    └─> CopyOnWriteArrayList

Need a shared map under heavy concurrency?
    └─> ConcurrentHashMap

Need producer/consumer hand-off (with optional priority ordering)?
    └─> BlockingQueue / PriorityBlockingQueue

Need exactly one lazily-created instance?
    └─> Double-checked locking with a volatile instance field
        (or an enum / static holder idiom)
```
