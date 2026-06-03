# CLAUDE.md — LLD Interview Preparation Repository

## What This Repo Is

A collection of Low Level Design (LLD) problems commonly asked in software engineering interviews. Each problem is a standalone Java 17 / Maven project in its own subfolder. The goal is clean, production-style OOP code that demonstrates design patterns, SOLID principles, and proper class relationships.

## Repository Structure

```
LLD/
├── CLAUDE.md                  # This file — project-wide context for Claude
├── README.md                  # Repo overview and question index
├── UML-ARROWS-GUIDE.md        # Shared UML relationship reference
├── .gitignore                 # Root-level gitignore for all subprojects
│
├── parking-lot/               # Question 1: Parking Lot System
│   ├── pom.xml
│   ├── README.md              # Problem-specific docs with flow + diagram
│   └── src/main/java/com/parkinglot/
│       ├── ParkingLotDemo.java
│       ├── model/
│       ├── enums/
│       └── strategy/
│
└── <future-question>/         # Each new LLD question follows same layout
```

## Standard Package Structure (Follow for Every Question)

Every LLD question MUST use this package layout under `src/main/java/com/<problemname>/`:

```
com/<problemname>/
├── <ProblemName>Demo.java       # Entry point with main() — always at package root
│
├── model/                       # Domain models and entities
│   ├── <CoreEntity>.java        # Main classes (e.g., ParkingLot, Elevator)
│   ├── <SubEntity>.java         # Supporting entities (e.g., ParkingFloor, ParkingSpot)
│   └── <DomainObject>.java      # Domain objects (e.g., Vehicle, Car, Truck)
│
├── enums/                       # All enumerations
│   └── <EnumName>.java          # e.g., VehicleSize, ElevatorDirection, OrderStatus
│
├── strategy/                    # Strategy pattern (if applicable)
│   ├── <Strategy>Strategy.java  # Interface (e.g., FeeStrategy, SchedulingStrategy)
│   └── <Impl>Strategy.java      # Implementations (e.g., VehicleBasedFeeStrategy)
│
├── observer/                    # Observer pattern (if applicable)
│   ├── <Event>Observer.java     # Observer interface
│   └── <Concrete>Observer.java  # Concrete observers
│
├── state/                       # State pattern (if applicable)
│   ├── <Entity>State.java       # State interface
│   └── <Concrete>State.java     # Concrete states
│
├── factory/                     # Factory pattern (if applicable)
│   └── <Entity>Factory.java     # Factory classes
│
├── command/                     # Command pattern (if applicable)
│   ├── Command.java             # Command interface
│   └── <Concrete>Command.java   # Concrete commands
│
├── decorator/                   # Decorator pattern (if applicable)
│   ├── <Base>Decorator.java     # Abstract decorator
│   └── <Concrete>Decorator.java # Concrete decorators
│
└── exception/                   # Custom exceptions (if needed)
    └── <Custom>Exception.java
```

**Rules:**
- Only create pattern folders that the question actually uses — don't create empty folders
- `model/` and `enums/` will exist in virtually every question
- The Demo class stays at package root, everything else goes in sub-packages
- Name pattern folders after the pattern itself (`strategy/`, `observer/`, `state/`), not the domain concept

## Coding Standards

### Language & Build
- Java 17 (LTS), Maven build
- Each question is an independent Maven module with its own `pom.xml`
- Package naming: `com.<problemname>` (e.g., `com.parkinglot`, `com.elevatorsystem`)
- Main class naming: `<ProblemName>Demo.java` with `public static void main`

### Code Style
- Follow standard Java conventions (camelCase methods/fields, PascalCase classes, UPPER_SNAKE enums)
- Fields are `private final` wherever possible (immutability by default)
- Use `Optional` for nullable returns instead of returning `null`
- Use enums for fixed sets of values (e.g., VehicleSize, not String constants)
- Prefer composition over inheritance
- No Lombok, no Spring — plain Java to show understanding, not framework magic
- No getters/setters unless genuinely needed by external callers

### Thread Safety
- Use `synchronized` on methods that mutate shared state
- Use `volatile` for double-checked locking (Singleton)
- Keep synchronized blocks as small as possible

## Design Principles (SOLID)

Every LLD solution must consciously apply:

1. **Single Responsibility (SRP)** — Each class has one reason to change
   - e.g., `ParkingSpot` manages spot state, `FeeStrategy` handles pricing, `ParkingTicket` tracks time
2. **Open/Closed (OCP)** — Open for extension, closed for modification
   - e.g., New fee logic = new `FeeStrategy` impl, no changes to `ParkingLot`
   - e.g., New vehicle type = new subclass + enum value, no changes to existing classes
3. **Liskov Substitution (LSP)** — Subtypes must be substitutable for their base types
   - e.g., `Car`, `Motorcycle`, `Truck` all work wherever `Vehicle` is expected
4. **Interface Segregation (ISP)** — Prefer small, focused interfaces
   - e.g., `FeeStrategy` has a single method, not a god interface
5. **Dependency Inversion (DIP)** — Depend on abstractions, not concretions
   - e.g., `ParkingLot` depends on `FeeStrategy` interface, not `VehicleBasedFeeStrategy`

## Design Patterns — When to Use

| Pattern | When to Use | Example |
|---------|------------|---------|
| **Singleton** | System-wide single instance (ParkingLot, ElevatorSystem) | Double-checked locking with `volatile` |
| **Strategy** | Swappable algorithms (fee calc, scheduling, sorting) | `FeeStrategy` interface |
| **Factory** | Complex object creation with variants | Vehicle factory, spot factory |
| **Observer** | Event notifications (spot freed, order status change) | Notify display boards |
| **State** | Object behavior changes with internal state | Elevator states, vending machine |
| **Command** | Encapsulate requests as objects | Undo/redo, remote control |
| **Builder** | Complex object construction with many optional params | Order builder, query builder |
| **Template Method** | Skeleton algorithm with customizable steps | Abstract base with hook methods |
| **Decorator** | Add behavior dynamically without subclassing | Toppings on pizza/coffee |
| **Chain of Responsibility** | Pass request through handler chain | Log levels, approval chains |

## README Convention for Each Question

Every LLD question folder MUST have a `README.md` with these sections in order:

```markdown
# <Problem Name> — Low Level Design

<One-line description>

## Problem Statement
<What the system should do — bullet points>

## High-Level Flow
<ASCII flowchart showing the main operations step by step>

## Class Diagram
<Excalidraw link for interactive viewing>
<Embedded PNG image (exported at 3x from Excalidraw) for quick reference>
<Mermaid `classDiagram` block inside a collapsed <details> with summary "Mermaid Class Diagram (click to expand)" — covers every class with fields/methods and all relationships (--|>, *--, o--, ..>, -->)>

## How to Approach This Problem (Smallest → Biggest)
<MANDATORY. A layered walkthrough that builds the design from the smallest insight upward.
Each "### Layer N: <pithy title>" paragraph explains ONE design decision and *why* it was made
that way — not what the code does. End with an "### Interview summary (say this verbatim)"
blockquote that compresses the whole design into a single spoken-aloud paragraph.
This section is the interview-prep core of the repo — never skip it.>

## Project Structure
<Tree view of the folder with one-line descriptions per file>

## Design Patterns Used
<Table: Pattern | Where | Why>

## SOLID Principles Applied
<Table: Principle | How it's applied in this specific solution>

## Thread Safety
<What's synchronized and why>

## Extensibility
<How to add new features without modifying existing code>
```

### Section style rules (apply to every README)

- **"How to Approach This Problem (Smallest → Biggest)"** is mandatory. Build the design layer by layer:
  - Layer 1 should be the smallest, most surprising insight (e.g. "a snake and a ladder are the same thing"). Each later layer adds one decision on top.
  - Explain *why* each choice — Liskov, SRP, race conditions, testability, etc. — not what the method does.
  - Call out the interview-grade trap or requirement in its own layer (concurrency, locking, ambiguity between similar-sounding entities).
  - Close with a single blockquoted "Interview summary (say this verbatim)" paragraph that a candidate could speak in 60–90 seconds.
- **Class Diagram section** must include BOTH the exported `img.png` and a collapsed Mermaid `classDiagram` block — Mermaid is the in-GitHub fallback when the PNG hasn't been regenerated yet.
- **No "How to Build & Run" section** anywhere — running is left to the IDE.

## Conventions for New Questions

When adding a new LLD question:

1. Create a new folder: `<question-name>/` (kebab-case)
2. Add a `pom.xml` with `groupId=com.<questionname>`, Java 17
3. Add a `README.md` following the template above (including Excalidraw diagram link)
4. Use standard package structure: `model/`, `enums/`, `strategy/` (+ others as needed)
5. Include a `<ProblemName>Demo.java` as the runnable entry point at package root
6. Update root `README.md` question table
7. Apply SOLID, pick design patterns that naturally fit the problem
8. **Create diagram assets** (ALL THREE are mandatory):
   - `class-diagram.excalidraw` — interactive Excalidraw source file at the question root. **Claude must generate this file** using the Excalidraw JSON format (see existing `.excalidraw` files for reference). Include all classes as color-coded rectangles with fields/methods, and arrows for relationships (dotted for implements, solid for extends/composition, labeled with relationship type).
   - `src/main/resources/img.png` — PNG exported from Excalidraw at **3x** scale, embedded in README via `![img.png](src/main/resources/img.png)` (user exports this manually from the `.excalidraw` file)
   - **Mermaid `classDiagram`** block inside the README's Class Diagram section, wrapped in a collapsed `<details>` element (`<summary>Mermaid Class Diagram (click to expand)</summary>`). This is the in-GitHub fallback and must cover the same classes and relationships as the Excalidraw source.
9. **Write the "How to Approach This Problem (Smallest → Biggest)" section** — see the section style rules above. This is non-negotiable and is the core differentiator of this repo from a generic LLD codebase.

## Class Relationship Guidelines

See [UML-ARROWS-GUIDE.md](UML-ARROWS-GUIDE.md) for full reference. Key rules:
- **Composition** (filled diamond): parent owns child, child dies with parent
- **Aggregation** (hollow diamond): parent holds child, child lives independently
- **Association** (arrow): knows-about reference, no ownership
- Use inheritance sparingly — prefer composition + interfaces

## What NOT to Include

- No Spring/Spring Boot — this is pure OOP design
- No database/persistence layer — in-memory only
- No REST endpoints — console demo is sufficient
- No Lombok — write explicit code to show understanding
- No `.claude/` folder in commits (gitignored)
