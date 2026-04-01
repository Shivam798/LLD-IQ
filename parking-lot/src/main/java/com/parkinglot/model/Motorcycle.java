package com.parkinglot.model;

import com.parkinglot.enums.VehicleSize;

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleSize.SMALL);
    }
}
