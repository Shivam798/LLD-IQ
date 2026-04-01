package com.parkinglot.model;

import com.parkinglot.enums.VehicleSize;

public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleSize.MEDIUM);
    }
}
