# Low Level Design (LLD) — Interview Preparation

This repo covers all the commonly asked Low Level Design questions for software engineering interviews. Each question is implemented as a standalone Java/Maven project in its own subfolder with clean OOP, design patterns, and UML diagrams.

## Questions Covered

| # | Problem | Folder |
|---|---------|--------|
| 1 | Parking Lot System | [`parking-lot/`](parking-lot/) |
| 2 | Coffee Vending Machine | [`coffee-vending-machine/`](coffee-vending-machine/) |
| 3 | Logging Framework | [`logging-framework/`](logging-framework/) |
| 4 | Pub-Sub System | [`pub-sub-system/`](pub-sub-system/) |
| 5 | ATM System | [`atm/`](atm/) |
| 6 | Elevator System | [`elevator-system/`](elevator-system/) |
| 7 | Library Management System | [`library-management-system/`](library-management-system/) |
| 8 | Movie Ticket Booking System | [`movie-ticket-booking-system/`](movie-ticket-booking-system/) |
| 9 | Splitwise | [`splitwise/`](splitwise/) |
| 10 | Snake and Ladder Game | [`snake-and-ladder/`](snake-and-ladder/) |
| 11 | Cache with pluggable eviction (LRU / LFU / FIFO) + TTL | [`lru-cache/`](lru-cache/) |
| 12 | Rate Limiter (with Token Bucket / Sliding Window strategies) | [`rate-limiter/`](rate-limiter/) |
| 13 | Notification Service (multi-channel: Email / SMS / Push) | [`notification-service/`](notification-service/) |
| 14 | Meeting Room Booking (interval overlap + allocation strategy) | [`meeting-room-booking/`](meeting-room-booking/) |
| 15 | Task Scheduler (cron-like: priority queue + dispatcher + worker pool) | [`task-scheduler/`](task-scheduler/) |

## Common Resources

- [UML Arrows & Relationship Guide](UML-ARROWS-GUIDE.md) — quick reference for reading class diagrams
- [Concurrency & Thread-Safety Guide](CONCURRENCY-GUIDE.md) — `volatile`, `synchronized`, `Atomic*`, CAS, CPU cache, concurrent collections + rapid-fire interview Q&A

## How to Run Any Question

Use your IDE's run button on the `*Demo.java` file in any question folder.
