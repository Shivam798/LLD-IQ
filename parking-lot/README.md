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

## How to Build & Run

### Using Maven
```bash
mvn clean package
java -jar target/parking-lot-1.0.0.jar
```

### Using javac directly
```bash
javac -d target/classes \
    src/main/java/com/parkinglot/enums/*.java \
    src/main/java/com/parkinglot/model/*.java \
    src/main/java/com/parkinglot/strategy/*.java \
    src/main/java/com/parkinglot/*.java

java -cp target/classes com.parkinglot.ParkingLotDemo
```

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
