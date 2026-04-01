package com.parkinglot.strategy;

import com.parkinglot.model.ParkingTicket;
import com.parkinglot.enums.VehicleSize;

public interface FeeStrategy {
    double calculateFee(ParkingTicket ticket, VehicleSize vehicleSize);
}
