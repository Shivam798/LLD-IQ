package com.parkinglot;

import com.parkinglot.enums.VehicleSize;
import com.parkinglot.model.*;
import com.parkinglot.strategy.VehicleBasedFeeStrategy;

import java.util.Optional;

public class ParkingLotDemo {

    public static void main(String[] args) {

        ParkingLot parkingLot = ParkingLot.getInstance();
        parkingLot.setFeeStrategy(new VehicleBasedFeeStrategy());

        // Floor 1: 1 small + 1 medium + 1 large spot
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new ParkingSpot("F1-S1", VehicleSize.SMALL));
        floor1.addSpot(new ParkingSpot("F1-M1", VehicleSize.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-L1", VehicleSize.LARGE));

        // Floor 2: 2 medium spots
        ParkingFloor floor2 = new ParkingFloor(2);
        floor2.addSpot(new ParkingSpot("F2-M1", VehicleSize.MEDIUM));
        floor2.addSpot(new ParkingSpot("F2-M2", VehicleSize.MEDIUM));

        parkingLot.addFloor(floor1);
        parkingLot.addFloor(floor2);

        // Create vehicles
        Vehicle car1   = new Car("KA-01-1234");
        Vehicle moto1  = new Motorcycle("KA-02-5678");
        Vehicle truck1 = new Truck("KA-03-9012");
        Vehicle car2   = new Car("KA-04-3456");
        Vehicle car3   = new Car("KA-05-7890");

        // Park vehicles
        ParkingTicket tCar1  = tryPark(parkingLot, car1);
        ParkingTicket tMoto  = tryPark(parkingLot, moto1);
        ParkingTicket tTruck = tryPark(parkingLot, truck1);
        ParkingTicket tCar2  = tryPark(parkingLot, car2);
        ParkingTicket tCar3  = tryPark(parkingLot, car3); // should fail

        System.out.println("---");

        // Unpark and show fees
        if (tCar1 != null) {
            double fee = parkingLot.unparkVehicle(tCar1);
            System.out.printf("Unparked %s — Fee: Rs.%.1f%n", car1, fee);
        }
        if (tTruck != null) {
            double fee = parkingLot.unparkVehicle(tTruck);
            System.out.printf("Unparked %s — Fee: Rs.%.1f%n", truck1, fee);
        }

        // Show availability
        for (ParkingFloor floor : parkingLot.getFloors()) {
            System.out.printf("Available spots on Floor %d: %d%n",
                    floor.getFloorNumber(), floor.getAvailableSpotCount());
        }
    }

    private static ParkingTicket tryPark(ParkingLot lot, Vehicle vehicle) {
        Optional<ParkingTicket> result = lot.parkVehicle(vehicle);
        if (result.isPresent()) {
            ParkingTicket t = result.get();
            System.out.printf("Parked %s at spot %s on floor %d%n",
                    vehicle, t.getSpot().getSpotId(), t.getFloorNumber());
            return t;
        } else {
            System.out.printf("No spot available for %s%n", vehicle);
            return null;
        }
    }
}
