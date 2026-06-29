package com.meetingroombooking.model;

import com.meetingroombooking.enums.Amenity;
import com.meetingroombooking.exception.RoomNotAvailableException;
import com.meetingroombooking.observer.BookingObserver;
import com.meetingroombooking.strategy.RoomAllocationStrategy;
import com.meetingroombooking.strategy.SmallestFitStrategy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * The orchestrator — the single entry point for booking and cancelling rooms.
 *
 * <p>SINGLETON (double-checked locking with a {@code volatile} instance): the
 * service is a system-wide facility owning the room registry, the allocation
 * strategy and the observer list.
 *
 * <p>It is deliberately thin. It does NOT know how overlap is detected (the
 * room does), how a room is chosen (the strategy does), or what happens after a
 * booking (observers do). Its job is to filter candidates, delegate the atomic
 * reservation to the room, and broadcast the result.
 */
public class BookingService {

    private static volatile BookingService instance;

    // Concurrent because rooms are read on every booking from many threads and
    // registered at startup; per-entry atomicity is all we need here.
    private final Map<String, MeetingRoom> rooms = new ConcurrentHashMap<>();

    // Read on every booking, written rarely (startup) -> CopyOnWriteArrayList.
    private final List<BookingObserver> observers = new CopyOnWriteArrayList<>();

    // Swappable at runtime; volatile so a reassignment is visible to all threads.
    private volatile RoomAllocationStrategy allocationStrategy;

    private BookingService() {
        this.allocationStrategy = new SmallestFitStrategy();
    }

    public static BookingService getInstance() {
        if (instance == null) {
            synchronized (BookingService.class) {
                if (instance == null) {
                    instance = new BookingService();
                }
            }
        }
        return instance;
    }

    public void addRoom(MeetingRoom room) {
        rooms.put(room.getId(), room);
    }

    public void addObserver(BookingObserver observer) {
        observers.add(observer);
    }

    public void setAllocationStrategy(RoomAllocationStrategy strategy) {
        this.allocationStrategy = strategy;
    }

    /**
     * Every room that is free for {@code slot}, large enough for {@code headcount}
     * and carries all {@code requiredAmenities}. A read-only query used both by
     * callers ("show me what's free") and internally by {@link #book}.
     */
    public List<MeetingRoom> findAvailableRooms(TimeSlot slot, int headcount, Set<Amenity> requiredAmenities) {
        Set<Amenity> required = (requiredAmenities == null) ? EnumSet.noneOf(Amenity.class) : requiredAmenities;
        return rooms.values().stream()
                .filter(room -> room.canSeat(headcount))
                .filter(room -> room.hasAmenities(required))
                .filter(room -> room.isAvailable(slot))
                .collect(Collectors.toList());
    }

    /**
     * Book a room for the meeting. Filters candidates, lets the strategy pick the
     * best one, then reserves it ATOMICALLY via {@link MeetingRoom#book}.
     *
     * <p>The subtlety: between {@code findAvailableRooms} and the reservation,
     * another thread might grab the chosen room. The room's {@code book()} does
     * its own check-then-act under the room lock and returns false if it lost the
     * race — so we simply drop that room and try the next best candidate. The
     * room lock, not the availability query, is the real guarantee against
     * double-booking.
     */
    public Booking book(User organizer, List<User> attendees, TimeSlot slot, Set<Amenity> requiredAmenities) {
        int headcount = 1 + (attendees == null ? 0 : attendees.size());
        List<MeetingRoom> candidates = new ArrayList<>(findAvailableRooms(slot, headcount, requiredAmenities));

        while (!candidates.isEmpty()) {
            Optional<MeetingRoom> chosen = allocationStrategy.selectRoom(candidates, headcount);
            if (chosen.isEmpty()) {
                break;
            }
            MeetingRoom room = chosen.get();
            Booking booking = new Booking.Builder(room, organizer, slot)
                    .attendees(attendees)
                    .build();

            if (room.book(booking)) {                 // atomic check-then-act inside the room
                observers.forEach(o -> o.onBookingConfirmed(booking));
                return booking;
            }
            candidates.remove(room);                  // lost the race — try the next best room
        }
        throw new RoomNotAvailableException(
                "No room available for " + slot + " (headcount " + headcount + ")");
    }

    /** Release a booking and tell observers; the room's slot becomes free. */
    public void cancel(Booking booking) {
        booking.getRoom().cancel(booking);
        booking.setStatus(com.meetingroombooking.enums.BookingStatus.CANCELLED);
        observers.forEach(o -> o.onBookingCancelled(booking));
    }
}
