package com.meetingroombooking.strategy;

import com.meetingroombooking.model.MeetingRoom;

import java.util.List;
import java.util.Optional;

/**
 * STRATEGY: given the rooms that are free and big enough for a request, pick
 * which one to actually use. Separating "which rooms are available" (the
 * service's job) from "which one do we choose" (this) keeps the selection
 * policy swappable — smallest-fit today, cost-based or floor-preference
 * tomorrow — without touching the booking flow (Open/Closed).
 */
public interface RoomAllocationStrategy {

    /**
     * @param availableRooms rooms already filtered to be free + amenity-matched
     * @param headcount      number of people to seat
     * @return the chosen room, or empty if none fits
     */
    Optional<MeetingRoom> selectRoom(List<MeetingRoom> availableRooms, int headcount);
}
