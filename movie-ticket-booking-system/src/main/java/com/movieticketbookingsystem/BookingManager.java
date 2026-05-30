package com.movieticketbookingsystem;

import com.movieticketbookingsystem.enums.PaymentStatus;
import com.movieticketbookingsystem.enums.SeatStatus;
import com.movieticketbookingsystem.model.Booking;
import com.movieticketbookingsystem.model.Payment;
import com.movieticketbookingsystem.model.Seat;
import com.movieticketbookingsystem.model.Show;
import com.movieticketbookingsystem.model.User;
import com.movieticketbookingsystem.strategy.payment.PaymentStrategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the end-to-end booking workflow: seat locking → pricing → payment → confirmation.
 * Acts as the transactional boundary that guarantees seats are never double-booked and
 * never left "stuck" if payment fails.
 */
public class BookingManager {
    // Delegates seat reservation to a dedicated component (SRP) — locks have TTL so abandoned
    // checkouts auto-expire without manual cleanup.
    private final SeatLockManager seatLockManager;

    // ConcurrentHashMap because bookings can be created/read from multiple threads
    // (different users booking concurrently). Gives us thread-safe puts/gets without
    // explicit synchronization on the map itself.
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    public BookingManager(SeatLockManager seatLockManager) {
        // Dependency injection — BookingManager depends on the SeatLockManager abstraction
        // rather than constructing it itself (DIP).
        this.seatLockManager = seatLockManager;
    }

    /**
     * Attempts to book the given seats for a user. Returns Optional.empty() if either
     * the seats cannot be locked (already taken / locked by someone else) or payment fails.
     * Strategy pattern: PaymentStrategy is passed in, so the manager doesn't care whether
     * it's UPI, card, or wallet — new payment methods don't require changes here (OCP).
     */
    public Optional<Booking> createBooking(User user, Show show, List<Seat> seats,
                                           PaymentStrategy paymentStrategy) {
        // Step 1: Acquire a short-lived lock on all requested seats BEFORE charging the user.
        // This prevents two users from paying for the same seat — classic race condition guard.
        // If even one seat can't be locked, abort the whole booking (all-or-nothing).
        if (!seatLockManager.lockSeats(show, seats, user.getId())) {
            return Optional.empty();
        }

        // Step 2: Price is computed via Show's pricing strategy (could be flat, dynamic,
        // weekend surge, etc.) — BookingManager stays agnostic to pricing rules.
        double totalAmount = show.getPricingStrategy().calculatePrice(seats);

        // Step 3: Attempt payment. The lock from step 1 protects the seats during this
        // network-bound call so no other user can grab them mid-payment.
        Payment payment = paymentStrategy.pay(totalAmount);

        // Step 4: If payment failed, roll back — release the lock so the seats become
        // immediately available again instead of waiting for the lock TTL to expire.
        // Also reset seat status to AVAILABLE in case it was flipped during locking.
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            System.out.println("Payment failed — releasing locked seats.");
            seatLockManager.unlockSeats(show, seats, user.getId());
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.AVAILABLE);
            }
            return Optional.empty();
        }

        // Step 5: Payment succeeded — build the Booking via Builder pattern (clean way
        // to construct an object with many fields without a giant constructor).
        Booking booking = new Booking.Builder()
                .user(user)
                .show(show)
                .seats(seats)
                .totalAmount(totalAmount)
                .payment(payment)
                .build();

        // Step 6: Mark the booking confirmed (flips seat status to BOOKED permanently).
        // After this point, the seats belong to this booking — the temporary lock is no
        // longer needed, so we release it.
        booking.confirmBooking();
        seatLockManager.unlockSeats(show, seats, user.getId());

        // Step 7: Persist the booking in our in-memory store so it can be retrieved later
        // (cancellation, ticket lookup, etc.).
        bookings.put(booking.getId(), booking);
        return Optional.of(booking);
    }

    /**
     * Lookup a previously created booking by id. Returns Optional to avoid null checks
     * at call sites — caller is forced to handle the "not found" case explicitly.
     */
    public Optional<Booking> getBooking(String bookingId) {
        return Optional.ofNullable(bookings.get(bookingId));
    }
}
