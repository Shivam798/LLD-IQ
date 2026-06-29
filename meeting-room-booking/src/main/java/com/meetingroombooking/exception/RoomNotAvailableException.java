package com.meetingroombooking.exception;

/**
 * Thrown when no room can satisfy a booking request — either nothing matches
 * the capacity/amenity filter, or every candidate was taken for the slot.
 *
 * Unchecked: a failed booking is an expected business outcome the caller
 * handles, not a programming error, but forcing a checked exception on every
 * call site would be noise. The caller decides whether to retry or surface it.
 */
public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
