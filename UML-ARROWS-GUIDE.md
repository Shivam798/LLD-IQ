# UML Class Diagram — Arrow & Relationship Guide

A quick reference for understanding the arrows used in the class diagrams of this project.

---

## Decision Flowchart

```
Does class A use class B?
│
├─ Only in a method body (parameter / local var)?  ──> DEPENDENCY
│
├─ Stores B as a field?
│   ├─ A creates & owns B, B dies with A?          ──> COMPOSITION
│   ├─ A holds B, but B lives independently?        ──> AGGREGATION
│   └─ General reference, no clear ownership?       ──> ASSOCIATION
│
├─ A extends B?                                     ──> INHERITANCE
│
└─ A implements B?                                  ──> REALIZATION
```

---

## 1. Inheritance — `──▷` (Solid line + hollow triangle)

**"Is-a" relationship.** A child class extends a parent class.

```
Car ──▷ Vehicle
Motorcycle ──▷ Vehicle
Truck ──▷ Vehicle
```

```java
public class Car extends Vehicle { }
```

**Ask yourself:** Can I say "A **is a** B"? → Inheritance.

---

## 2. Realization / Implementation — `╌╌▷` (Dashed line + hollow triangle)

**A class implements an interface.** Same triangle as inheritance but dashed because interfaces are contracts, not concrete parents.

```
VehicleBasedFeeStrategy ╌╌▷ «interface» FeeStrategy
```

```java
public class VehicleBasedFeeStrategy implements FeeStrategy { }
```

---

## 3. Composition — `◆──>` (Solid line + filled diamond)

**"Part-of" with ownership.** The part cannot exist without the whole. If the parent is destroyed, the children are destroyed too.

```
ParkingLot ◆──1..*──> ParkingFloor
ParkingFloor ◆──1..*──> ParkingSpot
```

```java
public class ParkingLot {
    // floors are created and owned here — they die when ParkingLot dies
    private final List<ParkingFloor> floors = new ArrayList<>();
}
```

**Ask yourself:** "If I delete the parent, does the child make sense on its own?" If **no** → Composition.

> The filled diamond sits on the **owner** (whole) side.

---

## 4. Aggregation — `◇──>` (Solid line + hollow diamond)

**"Has-a" without ownership.** The part can exist independently of the whole.

```
ParkingSpot ◇──> Vehicle
```

```java
public class ParkingSpot {
    // vehicle comes from outside, it exists before parking and after unparking
    private Vehicle parkedVehicle;
}
```

**Ask yourself:** "If I delete the parent, does the child still make sense?" If **yes** → Aggregation.

> The hollow diamond sits on the container side.

---

## 5. Association — `──>` (Solid line + open arrow)

**General "knows about" relationship.** One class holds a reference to another as a field, but there is no ownership or part-whole semantics.

```
ParkingTicket ──> Vehicle
ParkingTicket ──> ParkingSpot
ParkingLot ──> FeeStrategy
```

```java
public class ParkingTicket {
    private final Vehicle vehicle;    // references it, doesn't own it
    private final ParkingSpot spot;   // references it, doesn't own it
}
```

---

## 6. Dependency — `╌╌>` (Dashed line + open arrow)

**Weakest relationship.** One class uses another temporarily — in a method parameter, local variable, or static call — but does not store it as a field.

```
ParkingLotDemo ╌╌> ParkingLot
```

```java
public class ParkingLotDemo {
    public static void main(String[] args) {
        ParkingLot lot = ParkingLot.getInstance(); // uses it, doesn't hold it as a field
    }
}
```

---

## Visual Summary

| Arrow | Name | Line | Arrowhead | Keyword | Strength |
|-------|------|------|-----------|---------|----------|
| `──▷` | Inheritance | Solid | Hollow triangle | `extends` | Strong |
| `╌╌▷` | Realization | Dashed | Hollow triangle | `implements` | Strong |
| `◆──>` | Composition | Solid | Filled diamond | owns / creates | Strong |
| `◇──>` | Aggregation | Solid | Hollow diamond | has / holds | Medium |
| `──>` | Association | Solid | Open arrow | references | Medium |
| `╌╌>` | Dependency | Dashed | Open arrow | uses temporarily | Weak |

---

## Applied to This Project

| Arrow | From | To | Why |
|-------|------|----|-----|
| `◆──>` Composition | `ParkingLot` | `ParkingFloor` | Lot owns floors; floors die with lot |
| `◆──>` Composition | `ParkingFloor` | `ParkingSpot` | Floor owns spots; spots die with floor |
| `◇──>` Aggregation | `ParkingSpot` | `Vehicle` | Spot holds vehicle temporarily; vehicle exists independently |
| `──▷` Inheritance | `Car`, `Motorcycle`, `Truck` | `Vehicle` | `extends Vehicle` |
| `╌╌▷` Realization | `VehicleBasedFeeStrategy` | `FeeStrategy` | `implements FeeStrategy` |
| `──>` Association | `ParkingTicket` | `Vehicle`, `ParkingSpot` | Ticket references them, doesn't own them |
| `╌╌>` Dependency | `ParkingLotDemo` | `ParkingLot` | Demo uses lot in `main()` only |

---

## The Hardest Distinction: Composition vs Aggregation vs Association

All three involve one class holding a reference to another. The difference is **lifecycle coupling**:

| Question | Composition | Aggregation | Association |
|----------|-------------|-------------|-------------|
| Does the parent **create** the child? | Usually yes | No | No |
| Does the child **die** with the parent? | Yes | No | No |
| Can the child **belong to multiple** parents? | No | Yes | Yes |
| Is it a **part-whole** relationship? | Yes | Yes | No |

> **Interview tip:** When in doubt between Aggregation and Association, either is acceptable. The critical distinction interviewers care about is **Composition vs the rest** — that's where lifecycle ownership matters.
