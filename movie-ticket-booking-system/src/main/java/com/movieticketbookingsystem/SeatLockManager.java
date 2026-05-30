package com.movieticketbookingsystem;

import com.movieticketbookingsystem.enums.SeatStatus;
import com.movieticketbookingsystem.model.Seat;
import com.movieticketbookingsystem.model.Show;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Holds short-lived, per-user locks on seats while a booking is in flight (i.e. between
 * "user clicked book" and "payment confirmed"). The lock is the mechanism that prevents
 * two users from racing to buy the same seat — without it, both could see the seat as
 * AVAILABLE, both could try to pay, and we'd double-book.
 *
 * Locks are bounded by a TTL: if a user abandons checkout or their network drops, the
 * seat doesn't stay frozen forever — a scheduled task releases it automatically.
 */
public class SeatLockManager {
    // Lock lifetime — kept short on purpose. Long enough for a normal payment flow,
    // short enough that abandoned carts don't block other users for long. In a real
    // system this would be a config value (e.g., 5–10 minutes); here it's tiny so the
    // demo can exercise expiry quickly.
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    // Two-level map: Show → (Seat → userId who locked it). Keyed by Show because locks
    // are scoped to a specific showing — the same seat number exists across many shows
    // and each must be tracked independently. ConcurrentHashMap so reads/writes from
    // different booking threads don't need external synchronization on the map itself.
    private final Map<Show, Map<Seat, String>> lockedSeats = new ConcurrentHashMap<>();

    // Single-threaded scheduler is enough — releasing a lock is a fast in-memory op,
    // and serializing them avoids extra contention. In production you'd size this to
    // expected lock volume or use a delay queue.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Tries to atomically lock all requested seats for a single user. All-or-nothing:
     * if even one seat is unavailable, no seat is locked. Returning false tells the
     * caller (BookingManager) to abort the booking before any payment is attempted.
     */
    public boolean lockSeats(Show show, List<Seat> seats, String userId) {
        // Synchronize on the Show object so concurrent lock attempts for the SAME show
        // are serialized, while different shows can still proceed in parallel. This is
        // a classic "lock striping by domain key" — finer-grained than a global lock,
        // coarser than per-seat, which is the right balance for this access pattern.
        synchronized (show) {
            // Pre-check pass: validate ALL seats first before mutating any state. This
            // gives us the atomic "all-or-nothing" guarantee — we never want to lock
            // 3 of 4 seats, fail on the 4th, and leave the user holding partial locks.
            for (Seat seat : seats) {
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    System.out.println("Seat " + seat.getId() + " is not available — lock rejected.");
                    return false;
                }
            }

            // Lazily create the inner per-show map on first lock for that show.
            // computeIfAbsent is atomic, so two threads creating locks on a brand-new
            // show won't accidentally create two competing inner maps.
            Map<Seat, String> showLocks = lockedSeats.computeIfAbsent(show, s -> new ConcurrentHashMap<>());
            for (Seat seat : seats) {
                // Flip the seat to LOCKED so other code paths (e.g., getAvailableSeats)
                // immediately stop treating it as available, even before this method returns.
                seat.setStatus(SeatStatus.LOCKED);
                showLocks.put(seat, userId);
            }

            // Schedule auto-release. Critically, the released task identifies the lock
            // by userId — so if the user finishes payment and the lock is already gone,
            // the task is a no-op (it won't accidentally release someone else's later lock).
            scheduler.schedule(() -> releaseExpiredLocks(show, seats, userId),
                    LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            System.out.println("Locked seats " + seats.stream().map(Seat::getId).toList()
                    + " for user " + userId);
            return true;
        }
    }

    /**
     * Explicitly release locks held by a specific user. Called by BookingManager on
     * both success (after confirming the booking — lock no longer needed) and failure
     * (payment failed — free seats immediately instead of waiting for TTL).
     */
    public void unlockSeats(Show show, List<Seat> seats, String userId) {
        synchronized (show) {
            Map<Seat, String> showLocks = lockedSeats.get(show);
            // Defensive null check — locks may have already expired and been cleaned up.
            if (showLocks == null) return;

            for (Seat seat : seats) {
                // Only release locks owned by this user. Prevents one user from
                // accidentally (or maliciously) unlocking another user's reservation.
                if (userId.equals(showLocks.get(seat))) {
                    showLocks.remove(seat);
                }
            }
            // Housekeeping: drop the inner map entirely if no locks remain for this
            // show, so the outer map doesn't accumulate empty entries over time.
            if (showLocks.isEmpty()) {
                lockedSeats.remove(show);
            }
        }
    }

    /**
     * Runs when a lock TTL expires. Mirrors unlockSeats but additionally resets the
     * Seat's status back to AVAILABLE — because expiry means the user never completed
     * the booking, so the seat needs to go back into the pool for others to grab.
     */
    private void releaseExpiredLocks(Show show, List<Seat> seats, String userId) {
        synchronized (show) {
            Map<Seat, String> showLocks = lockedSeats.get(show);
            if (showLocks == null) return;

            for (Seat seat : seats) {
                // Double guard: only release if (a) this user still owns the lock and
                // (b) the seat is still in LOCKED state. If the user already completed
                // their booking, the seat is now BOOKED and we must NOT flip it back.
                if (userId.equals(showLocks.get(seat)) && seat.getStatus() == SeatStatus.LOCKED) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    showLocks.remove(seat);
                    System.out.println("Lock expired on seat " + seat.getId() + " — released.");
                }
            }
            if (showLocks.isEmpty()) {
                lockedSeats.remove(show);
            }
        }
    }

    /**
     * Graceful shutdown — stops accepting new scheduled tasks, waits briefly for
     * in-flight ones, then force-kills if they hang. Always interrupt the current
     * thread on InterruptedException so callers up the stack know about the interrupt.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            // Preserve interrupt status — swallowing it would mask the signal from
            // higher-level shutdown logic that may be waiting on this thread.
            Thread.currentThread().interrupt();
        }
    }
}
