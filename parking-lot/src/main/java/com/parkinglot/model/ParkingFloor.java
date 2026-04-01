package com.parkinglot.model;

import com.parkinglot.enums.VehicleSize;

import java.util.ArrayList;
import java.util.List;

public class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }

    public ParkingSpot findAvailableSpot(VehicleSize size) {
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.canFit(size)) {
                return spot;
            }
        }
        return null;
    }

    public synchronized ParkingSpot parkVehicle(Vehicle vehicle) {
        ParkingSpot spot = findAvailableSpot(vehicle.getSize());
        if (spot != null) {
            spot.park(vehicle);
            return spot;
        }
        return null;
    }

    public synchronized boolean unparkVehicle(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable() && spot.getParkedVehicle().equals(vehicle)) {
                spot.unpark();
                return true;
            }
        }
        return false;
    }

    public int getFloorNumber()       { return floorNumber; }

    public int getAvailableSpotCount() {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable()) count++;
        }
        return count;
    }
}
