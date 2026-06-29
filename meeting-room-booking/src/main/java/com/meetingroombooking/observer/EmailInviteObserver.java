package com.meetingroombooking.observer;

import com.meetingroombooking.model.Booking;
import com.meetingroombooking.model.User;

/**
 * Emails a calendar invite to every attendee on confirmation, and a
 * cancellation notice when a booking is released. The simplest concrete
 * observer — its only job is to turn a lifecycle event into messages.
 */
public class EmailInviteObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(Booking booking) {
        System.out.println("  [INVITE] '" + booking.getRoom().getName() + "' "
                + booking.getSlot() + " — inviting:");
        for (User attendee : booking.getAttendees()) {
            System.out.println("            -> " + attendee.getName() + " <" + attendee.getEmail() + ">");
        }
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        System.out.println("  [INVITE] CANCELLED Booking#" + booking.getId()
                + " — notifying " + booking.getAttendees().size() + " attendee(s)");
    }
}
