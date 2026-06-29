package com.meetingroombooking.enums;

/**
 * Lifecycle state of a booking.
 *
 *   CONFIRMED -> the room is reserved for the slot
 *   CANCELLED -> the reservation was released; the slot is free again
 *
 * Observers react to these transitions (invite sent, calendar updated).
 */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
