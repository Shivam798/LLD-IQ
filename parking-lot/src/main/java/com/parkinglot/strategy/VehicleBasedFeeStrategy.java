package com.parkinglot.strategy;

import com.parkinglot.model.ParkingTicket;
import com.parkinglot.enums.VehicleSize;

/**
 * Charges different hourly rates based on vehicle size:
 *   SMALL  (Motorcycle) -> 10 / hour
 *   MEDIUM (Car)         -> 20 / hour
 *   LARGE  (Truck)       -> 30 / hour
 * Minimum charge is 1 hour.
 */
public class VehicleBasedFeeStrategy implements FeeStrategy {

    @Override
    public double calculateFee(ParkingTicket ticket, VehicleSize vehicleSize) {
        long hours = ticket.getDurationHours();

        double ratePerHour;
        switch (vehicleSize) {
            case SMALL:  ratePerHour = 10.0; break;
            case MEDIUM: ratePerHour = 20.0; break;
            case LARGE:  ratePerHour = 30.0; break;
            default:     ratePerHour = 20.0; break;
        }

        return hours * ratePerHour;
    }
}
