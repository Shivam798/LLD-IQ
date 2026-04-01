package com.parkinglot.model;

import com.parkinglot.enums.VehicleSize;

public class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleSize.LARGE);
    }
}
