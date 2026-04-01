package com.parkinglot.model;

import com.parkinglot.enums.VehicleSize;

public abstract class Vehicle {
    private final String licensePlate;
    private final VehicleSize size;

    protected Vehicle(String licensePlate, VehicleSize size) {
        this.licensePlate = licensePlate;
        this.size = size;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleSize getSize() {
        return size;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + licensePlate + "]";
    }
}
