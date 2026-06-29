package com.meetingroombooking;

import com.meetingroombooking.enums.Amenity;
import com.meetingroombooking.exception.RoomNotAvailableException;
import com.meetingroombooking.model.Booking;
import com.meetingroombooking.model.BookingService;
import com.meetingroombooking.model.MeetingRoom;
import com.meetingroombooking.model.TimeSlot;
import com.meetingroombooking.model.User;
import com.meetingroombooking.observer.CalendarSyncObserver;
import com.meetingroombooking.observer.EmailInviteObserver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Runnable walkthrough of the meeting room booking system.
 *
 * Scenarios:
 *   1. Smallest-fit allocation — a small meeting picks the tightest room.
 *   2. Double-booking is rejected — an overlapping request finds no room.
 *   3. Back-to-back booking is allowed — half-open intervals don't conflict.
 *   4. Filtered search — find rooms by capacity + amenities.
 *   5. Cancel frees the slot — the same slot can then be re-booked.
 */
public class MeetingRoomBookingDemo {

    public static void main(String[] args) {
        BookingService service = BookingService.getInstance();
        service.addObserver(new EmailInviteObserver());
        CalendarSyncObserver calendar = new CalendarSyncObserver();
        service.addObserver(calendar);

        // --- Rooms: a deliberate spread of capacity + amenities ----------------
        service.addRoom(new MeetingRoom("R1", "Focus",     2,  1, EnumSet.noneOf(Amenity.class)));
        service.addRoom(new MeetingRoom("R2", "Huddle",    4,  1, EnumSet.of(Amenity.WHITEBOARD)));
        service.addRoom(new MeetingRoom("R3", "Boardroom", 12, 2, EnumSet.of(Amenity.PROJECTOR, Amenity.VIDEO_CONFERENCE)));
        service.addRoom(new MeetingRoom("R4", "Auditorium", 50, 3, EnumSet.of(Amenity.PROJECTOR, Amenity.VIDEO_CONFERENCE, Amenity.CONFERENCE_PHONE)));

        // --- People ------------------------------------------------------------
        User alice = new User("u1", "Alice", "alice@corp.com");
        User bob = new User("u2", "Bob", "bob@corp.com");
        User carol = new User("u3", "Carol", "carol@corp.com");

        LocalDate day = LocalDate.of(2026, 7, 1);

        System.out.println("=== 1. Smallest-fit allocation (3 people, no amenities) ===");
        // Free + fits 3: Huddle(4), Boardroom(12), Auditorium(50). Smallest-fit -> Huddle.
        TimeSlot nineToTen = slot(day, 9, 0, 10, 0);
        Booking b1 = service.book(alice, List.of(bob, carol), nineToTen, null);
        System.out.println("  booked: " + b1);

        System.out.println("\n=== 2. Overlapping request for a whiteboard room -> rejected ===");
        // 09:30-10:30 needs a WHITEBOARD. Only Huddle has one, and it's taken 09-10.
        TimeSlot overlap = slot(day, 9, 30, 10, 30);
        try {
            service.book(bob, List.of(alice), overlap, EnumSet.of(Amenity.WHITEBOARD));
        } catch (RoomNotAvailableException ex) {
            System.out.println("  rejected: " + ex.getMessage());
        }

        System.out.println("\n=== 3. Back-to-back booking (10-11) on the same room -> allowed ===");
        // Half-open [09:00,10:00) and [10:00,11:00) do NOT overlap.
        TimeSlot tenToEleven = slot(day, 10, 0, 11, 0);
        Booking b3 = service.book(bob, List.of(carol), tenToEleven, EnumSet.of(Amenity.WHITEBOARD));
        System.out.println("  booked: " + b3 + "  (same room as #1: "
                + b3.getRoom().getName().equals(b1.getRoom().getName()) + ")");

        System.out.println("\n=== 4. Filtered search: rooms free 14-15 for 8 people with a PROJECTOR ===");
        TimeSlot afternoon = slot(day, 14, 0, 15, 0);
        List<MeetingRoom> matches = service.findAvailableRooms(afternoon, 8, EnumSet.of(Amenity.PROJECTOR));
        matches.forEach(r -> System.out.println("  candidate: " + r));
        Booking b4 = service.book(alice, List.of(bob, carol), afternoon, EnumSet.of(Amenity.PROJECTOR));
        System.out.println("  smallest-fit chose: " + b4.getRoom().getName());

        System.out.println("\n=== 5. Cancel frees the slot, then re-book it ===");
        service.cancel(b1);
        Booking b5 = service.book(carol, List.of(alice), nineToTen, EnumSet.of(Amenity.WHITEBOARD));
        System.out.println("  re-booked 09-10: " + b5);

        System.out.println("\n=== Active bookings (from CalendarSyncObserver): " + calendar.getActiveBookings() + " ===");
    }

    private static TimeSlot slot(LocalDate day, int sh, int sm, int eh, int em) {
        return new TimeSlot(
                LocalDateTime.of(day, LocalTime.of(sh, sm)),
                LocalDateTime.of(day, LocalTime.of(eh, em)));
    }
}
