package com.meetingroombooking.strategy;

import com.meetingroombooking.model.MeetingRoom;

import java.util.List;
import java.util.Optional;

/**
 * Pick the first room that fits — cheapest possible policy, useful when
 * minimizing allocation work matters more than packing rooms efficiently.
 * Demonstrates that swapping the allocation policy is a one-class change.
 */
public class FirstAvailableStrategy implements RoomAllocationStrategy {

    @Override
    public Optional<MeetingRoom> selectRoom(List<MeetingRoom> availableRooms, int headcount) {
        return availableRooms.stream()
                .filter(room -> room.canSeat(headcount))
                .findFirst();
    }
}
