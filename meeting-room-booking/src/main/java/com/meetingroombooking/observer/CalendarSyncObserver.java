package com.meetingroombooking.observer;

import com.meetingroombooking.model.Booking;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps a running count of active bookings by syncing each confirm/cancel to an
 * external calendar. Demonstrates that an observer can hold its OWN state the
 * service neither owns nor knows about.
 *
 * The counter is an {@link AtomicLong} because bookings may be confirmed and
 * cancelled from multiple threads, and increment/decrement must not race.
 */
public class CalendarSyncObserver implements BookingObserver {

    private final AtomicLong activeBookings = new AtomicLong();

    @Override
    public void onBookingConfirmed(Booking booking) {
        long now = activeBookings.incrementAndGet();
        System.out.println("  [CAL] synced Booking#" + booking.getId()
                + " (active bookings: " + now + ")");
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        long now = activeBookings.decrementAndGet();
        System.out.println("  [CAL] removed Booking#" + booking.getId()
                + " (active bookings: " + now + ")");
    }

    public long getActiveBookings() {
        return activeBookings.get();
    }
}
