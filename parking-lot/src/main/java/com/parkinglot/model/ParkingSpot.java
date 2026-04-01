package com.parkinglot.model;

import com.parkinglot.enums.VehicleSize;

public class ParkingSpot {
    private final String spotId;
    private final VehicleSize size;
    private Vehicle parkedVehicle;
    private boolean available;

    public ParkingSpot(String spotId, VehicleSize size) {
        this.spotId = spotId;
        this.size = size;
        this.available = true;
    }

    public synchronized boolean isAvailable() {
        return available;
    }

    public boolean canFit(VehicleSize vehicleSize) {
        return this.size == vehicleSize;
    }

    public synchronized void park(Vehicle vehicle) {
        if (!available) {
            throw new IllegalStateException("Spot " + spotId + " is already occupied");
        }
        this.parkedVehicle = vehicle;
        this.available = false;
    }

    public synchronized void unpark() {
        this.parkedVehicle = null;
        this.available = true;
    }

    public String getSpotId()          { return spotId; }
    public VehicleSize getSize()       { return size; }
    public Vehicle getParkedVehicle()  { return parkedVehicle; }
}
