package com.meetingroombooking.strategy;

import com.meetingroombooking.model.MeetingRoom;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Pick the SMALLEST room that still seats everyone — the "best-fit" policy.
 * <p>
 * Why this is the sensible default: putting a 3-person meeting in a 12-person
 * boardroom wastes the boardroom for anyone who later needs it. Choosing the
 * tightest-fitting room keeps the big rooms free for the big meetings, reducing
 * fragmentation — the same instinct as best-fit memory allocation.
 */
public class SmallestFitStrategy implements RoomAllocationStrategy {

    @Override
    public Optional<MeetingRoom> selectRoom(List<MeetingRoom> availableRooms, int headcount) {
        return availableRooms.stream()
                .filter(room -> room.canSeat(headcount))
                .min(Comparator.comparingInt(MeetingRoom::getCapacity));
    }
}
