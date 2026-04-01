package com.parkinglot.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final int floorNumber;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot, int floorNumber) {
        this.ticketId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.vehicle = vehicle;
        this.spot = spot;
        this.floorNumber = floorNumber;
        this.entryTime = LocalDateTime.now();
    }

    public void markExit() {
        this.exitTime = LocalDateTime.now();
    }

    public long getDurationHours() {
        LocalDateTime end = (exitTime != null) ? exitTime : LocalDateTime.now();
        long hours = Duration.between(entryTime, end).toHours();
        return Math.max(1, hours);
    }

    public String getTicketId()          { return ticketId; }
    public Vehicle getVehicle()          { return vehicle; }
    public ParkingSpot getSpot()         { return spot; }
    public int getFloorNumber()          { return floorNumber; }
    public LocalDateTime getEntryTime()  { return entryTime; }
    public LocalDateTime getExitTime()   { return exitTime; }
}
