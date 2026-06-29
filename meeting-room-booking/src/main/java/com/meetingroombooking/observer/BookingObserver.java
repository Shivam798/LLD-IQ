package com.meetingroombooking.observer;

import com.meetingroombooking.model.Booking;

/**
 * OBSERVER: reacts to booking lifecycle events. Sending invites, syncing
 * calendars and auditing are concerns SEPARATE from reserving a room, so they
 * live in observers the BookingService notifies — it never imports an email or
 * calendar client. New reaction = new observer, zero edits to the service.
 */
public interface BookingObserver {

    void onBookingConfirmed(Booking booking);

    void onBookingCancelled(Booking booking);
}
