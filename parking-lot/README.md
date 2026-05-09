# Parking Lot System — Low Level Design

A complete object-oriented design and Java implementation of a **Parking Lot Management System**.

---

## Problem Statement

Design a parking lot system that can:
- Manage multiple floors, each with multiple parking spots of different sizes
- Park and unpark vehicles (Car, Motorcycle, Truck) based on size compatibility
- Generate tickets on entry and calculate fees on exit
- Handle concurrent access safely
- Support pluggable fee calculation strategies

---

## High-Level Flow

```
Vehicle arrives
    │
    ▼
ParkingLot.parkVehicle(vehicle)
    │
    ├── Iterate floors ──> ParkingFloor.parkVehicle(vehicle)
    │                           │
    │                           ├── findAvailableSpot(vehicleSize)
    │                           │       │
    │                           │       └── spot.isAvailable() && spot.canFit(size)
    │                           │
    │                           └── spot.park(vehicle)
    │
    ├── Found spot ──> Create ParkingTicket ──> return ticket
    │
    └── No spot ──> return empty

    ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─

Vehicle exits
    │
    ▼
ParkingLot.unparkVehicle(ticket)
    │
    ├── ticket.getSpot().unpark()
    ├── ticket.markExit()
    └── feeStrategy.calculateFee(ticket, vehicleSize) ──> return fee
```

---

## Class Diagram

https://excalidraw.com/#json=fvtswdhfQckVGfT_XENf1,LNGZyP4X3YUOxj4kMeN_Kw

![Class Diagram](src/main/resources/img.png)

<details>
<summary>Mermaid Class Diagram (click to expand)</summary>

```mermaid
classDiagram
    direction TB

    class VehicleSize {
        <<enum>>
        SMALL
        MEDIUM
        LARGE
    }

    class Vehicle {
        <<abstract>>
        -licensePlate: String
        -size: VehicleSize
        +getLicensePlate() String
        +getSize() VehicleSize
        +toString() String
    }

    class Car {
    }

    class Motorcycle {
    }

    class Truck {
    }

    class ParkingSpot {
        -spotId: String
        -size: VehicleSize
        -parkedVehicle: Vehicle
        -available: boolean
        +isAvailable() boolean «sync»
        +canFit(VehicleSize) boolean
        +park(Vehicle) «sync»
        +unpark() «sync»
        +getSpotId() String
        +getSize() VehicleSize
        +getParkedVehicle() Vehicle
    }

    class ParkingFloor {
        -floorNumber: int
        -spots: List~ParkingSpot~
        +addSpot(ParkingSpot)
        +findAvailableSpot(VehicleSize) ParkingSpot
        +parkVehicle(Vehicle) ParkingSpot «sync»
        +unparkVehicle(Vehicle) boolean «sync»
        +getFloorNumber() int
        +getAvailableSpotCount() int
    }

    class ParkingTicket {
        -ticketId: String
        -vehicle: Vehicle
        -spot: ParkingSpot
        -floorNumber: int
        -entryTime: LocalDateTime
        -exitTime: LocalDateTime
        +markExit()
        +getDurationHours() long
        +getTicketId() String
        +getVehicle() Vehicle
        +getSpot() ParkingSpot
        +getFloorNumber() int
        +getEntryTime() LocalDateTime
        +getExitTime() LocalDateTime
    }

    class FeeStrategy {
        <<interface>>
        +calculateFee(ParkingTicket, VehicleSize) double
    }

    class VehicleBasedFeeStrategy {
        +calculateFee(ParkingTicket, VehicleSize) double
    }

    class ParkingLot {
        <<singleton>>
        -instance: ParkingLot
        -floors: List~ParkingFloor~
        -feeStrategy: FeeStrategy
        +getInstance() ParkingLot
        +addFloor(ParkingFloor)
        +setFeeStrategy(FeeStrategy)
        +parkVehicle(Vehicle) Optional~ParkingTicket~ «sync»
        +unparkVehicle(ParkingTicket) double «sync»
        +getFloors() List~ParkingFloor~
        +getTotalAvailableSpots() int
    }

    Car --|> Vehicle
    Motorcycle --|> Vehicle
    Truck --|> Vehicle

    VehicleBasedFeeStrategy ..|> FeeStrategy

    ParkingLot *-- ParkingFloor : floors
    ParkingLot --> FeeStrategy : feeStrategy
    ParkingFloor *-- ParkingSpot : spots

    ParkingSpot --> Vehicle : parkedVehicle
    ParkingSpot --> VehicleSize : size
    ParkingTicket --> Vehicle : vehicle
    ParkingTicket --> ParkingSpot : spot

    ParkingLot ..> ParkingTicket : creates
    Vehicle --> VehicleSize : size
```
</details>

---

## Project Structure

```
parking-lot/
├── pom.xml
├── README.md
└── src/main/java/com/parkinglot/
    ├── ParkingLotDemo.java              # Entry point (main)
    │
    ├── model/                           # Domain models / entities
    │   ├── ParkingLot.java              # Singleton — orchestrates the system
    │   ├── ParkingFloor.java            # Manages spots on one floor
    │   ├── ParkingSpot.java             # Individual spot with availability
    │   ├── ParkingTicket.java           # Ticket generated on entry
    │   ├── Vehicle.java                 # Abstract base class
    │   ├── Car.java                     # size = MEDIUM
    │   ├── Motorcycle.java              # size = SMALL
    │   └── Truck.java                   # size = LARGE
    │
    ├── enums/                           # Enumerations
    │   └── VehicleSize.java             # SMALL, MEDIUM, LARGE
    │
    └── strategy/                        # Strategy pattern implementations
        ├── FeeStrategy.java             # Strategy interface
        └── VehicleBasedFeeStrategy.java # Hourly rate by vehicle size
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `ParkingLot` | One parking lot instance system-wide (double-checked locking with `volatile`) |
| **Strategy** | `FeeStrategy` interface | Pluggable fee calculation — swap algorithms without changing `ParkingLot` |
| **Template Method** | `Vehicle` abstract class | Common state/behavior in base, concrete subclasses set their size |

---

## SOLID Principles Applied

| Principle | How |
|-----------|-----|
| **SRP** | `ParkingSpot` manages spot state, `ParkingTicket` tracks time, `FeeStrategy` handles pricing |
| **OCP** | New fee logic = new `FeeStrategy` impl. New vehicle = new subclass + enum value. No existing code changes. |
| **LSP** | `Car`, `Motorcycle`, `Truck` are all substitutable wherever `Vehicle` is used |
| **ISP** | `FeeStrategy` has one method — no bloated interfaces |
| **DIP** | `ParkingLot` depends on `FeeStrategy` interface, not `VehicleBasedFeeStrategy` concrete class |

---

## Thread Safety

- `parkVehicle()` and `unparkVehicle()` in `ParkingLot` are `synchronized`
- `park()`, `unpark()`, `isAvailable()` in `ParkingSpot` are `synchronized`
- Singleton uses `volatile` + double-checked locking

---

## Extensibility

- **New vehicle type** → add enum value in `VehicleSize` + new subclass extending `Vehicle`
- **New fee logic** → implement `FeeStrategy` interface
- **Entry/exit gates, observer pattern, payment** — all plug in cleanly without modifying existing classes
