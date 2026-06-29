package com.meetingroombooking.model;

import com.meetingroombooking.enums.Amenity;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * A bookable room. It OWNS its bookings — overlap detection lives here, not in
 * the service (Single Responsibility: a room is the authority on its own
 * availability).
 *
 * <p><b>Why a {@link TreeMap} keyed by start time?</b> A plain list would force
 * an O(n) scan of every booking on each availability check. A {@code TreeMap}
 * keeps bookings sorted by start, so we only ever inspect the two neighbours of
 * a candidate slot — the booking starting just before it and the one starting
 * just after — in O(log n).
 *
 * <p><b>Thread safety.</b> {@code book()} performs a check-then-act (is the slot
 * free? then take it). Two threads booking the same room for overlapping slots
 * could both pass the check and both insert. The fix is to make check+insert
 * atomic by {@code synchronized}-ing on the room. The lock is PER ROOM, so two
 * people booking different rooms never block each other.
 */
public class MeetingRoom {

    private final String id;
    private final String name;
    private final int capacity;
    private final int floor;
    private final Set<Amenity> amenities;

    // start time -> booking, sorted by start. Guarded by `this` lock.
    private final NavigableMap<LocalDateTime, Booking> bookings = new TreeMap<>();

    public MeetingRoom(String id, String name, int capacity, int floor, Set<Amenity> amenities) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.floor = floor;
        this.amenities = (amenities == null) ? EnumSet.noneOf(Amenity.class) : EnumSet.copyOf(amenities);
    }

    /**
     * Reserve this room for the booking's slot, atomically. Returns false if the
     * slot is already taken — the caller (BookingService) treats false as
     * "lost the race, try another room".
     */
    public synchronized boolean book(Booking booking) {
        if (overlaps(booking.getSlot())) {
            return false;
        }
        bookings.put(booking.getSlot().getStart(), booking);
        return true;
    }

    /** Release a previously confirmed booking, freeing its slot. */
    public synchronized void cancel(Booking booking) {
        bookings.remove(booking.getSlot().getStart());
    }

    /** Is this room free for the whole slot? (A point-in-time read.) */
    public synchronized boolean isAvailable(TimeSlot slot) {
        return !overlaps(slot);
    }

    /**
     * O(log n) overlap test using the two neighbouring bookings:
     *   - the booking starting at or before our start: does it run past our start?
     *   - the booking starting at or after our start: does it begin before our end?
     * Either case is a conflict. Relies on TimeSlot's half-open semantics.
     */
    private boolean overlaps(TimeSlot slot) {
        Map.Entry<LocalDateTime, Booking> before = bookings.floorEntry(slot.getStart());
        if (before != null && slot.overlaps(before.getValue().getSlot())) {
            return true;
        }
        Map.Entry<LocalDateTime, Booking> after = bookings.ceilingEntry(slot.getStart());
        return after != null && slot.overlaps(after.getValue().getSlot());
    }

    public boolean hasAmenities(Set<Amenity> required) {
        return amenities.containsAll(required);
    }

    public boolean canSeat(int headcount) {
        return capacity >= headcount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getFloor() {
        return floor;
    }

    public Set<Amenity> getAmenities() {
        return EnumSet.copyOf(amenities);
    }

    @Override
    public String toString() {
        return name + "(cap=" + capacity + ", floor=" + floor + ")";
    }
}
