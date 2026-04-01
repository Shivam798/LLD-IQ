package com.parkinglot.model;

import com.parkinglot.strategy.FeeStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParkingLot {

    private static volatile ParkingLot instance;

    public static ParkingLot getInstance() {
        if (instance == null) {
            synchronized (ParkingLot.class) {
                if (instance == null) {
                    instance = new ParkingLot();
                }
            }
        }
        return instance;
    }

    static void resetInstance() {
        synchronized (ParkingLot.class) {
            instance = null;
        }
    }

    private final List<ParkingFloor> floors;
    private FeeStrategy feeStrategy;

    private ParkingLot() {
        this.floors = new ArrayList<>();
    }

    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    public void setFeeStrategy(FeeStrategy feeStrategy) {
        this.feeStrategy = feeStrategy;
    }

    public synchronized Optional<ParkingTicket> parkVehicle(Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.parkVehicle(vehicle);
            if (spot != null) {
                ParkingTicket ticket = new ParkingTicket(vehicle, spot, floor.getFloorNumber());
                return Optional.of(ticket);
            }
        }
        return Optional.empty();
    }

    public synchronized double unparkVehicle(ParkingTicket ticket) {
        if (feeStrategy == null) {
            throw new IllegalStateException("No FeeStrategy configured on the ParkingLot");
        }
        ticket.getSpot().unpark();
        ticket.markExit();
        return feeStrategy.calculateFee(ticket, ticket.getVehicle().getSize());
    }

    public List<ParkingFloor> getFloors() {
        return floors;
    }

    public int getTotalAvailableSpots() {
        int total = 0;
        for (ParkingFloor floor : floors) {
            total += floor.getAvailableSpotCount();
        }
        return total;
    }
}
