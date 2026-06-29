package com.meetingroombooking.model;

import com.meetingroombooking.enums.BookingStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One reservation: a room held for a {@link TimeSlot} by an organizer, with a
 * list of attendees.
 *
 * <p>Built via the BUILDER pattern — room/organizer/slot are required (taken in
 * the builder's constructor so they can't be forgotten) while attendees are
 * optional. Everything is immutable except {@code status}, which flips to
 * CANCELLED when the reservation is released.
 *
 * <p>{@code status} is {@code volatile}: it's the one mutable field and may be
 * read by a thread other than the one that cancelled, so it needs the
 * visibility guarantee. It's a simple flag (no read-modify-write), so volatile
 * is sufficient — no lock required just for the status.
 */
public final class Booking {

    // Process-wide unique id source — AtomicLong so concurrent bookings never
    // collide on an id.
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(1);

    private final long id;
    private final MeetingRoom room;
    private final User organizer;
    private final List<User> attendees;
    private final TimeSlot slot;

    private volatile BookingStatus status;

    private Booking(Builder builder) {
        this.id = ID_SEQUENCE.getAndIncrement();
        this.room = builder.room;
        this.organizer = builder.organizer;
        this.slot = builder.slot;
        this.attendees = List.copyOf(builder.attendees);
        this.status = BookingStatus.CONFIRMED;
    }

    public long getId() {
        return id;
    }

    public MeetingRoom getRoom() {
        return room;
    }

    public User getOrganizer() {
        return organizer;
    }

    public List<User> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    /** Headcount the room must seat: the organizer plus every attendee. */
    public int occupancy() {
        return 1 + attendees.size();
    }

    @Override
    public String toString() {
        return "Booking#" + id + "[" + room.getName() + " " + slot
                + ", by " + organizer + ", " + occupancy() + " ppl, " + status + "]";
    }

    public static class Builder {
        private final MeetingRoom room;
        private final User organizer;
        private final TimeSlot slot;
        private List<User> attendees = new ArrayList<>();

        public Builder(MeetingRoom room, User organizer, TimeSlot slot) {
            if (room == null || organizer == null || slot == null) {
                throw new IllegalArgumentException("room, organizer and slot are required");
            }
            this.room = room;
            this.organizer = organizer;
            this.slot = slot;
        }

        public Builder attendees(List<User> attendees) {
            this.attendees = (attendees == null) ? new ArrayList<>() : attendees;
            return this;
        }

        public Booking build() {
            return new Booking(this);
        }
    }
}
