# Low Level Design (LLD) — Interview Preparation

This repo covers all the commonly asked Low Level Design questions for software engineering interviews. Each question is implemented as a standalone Java/Maven project in its own subfolder with clean OOP, design patterns, and UML diagrams.

## Questions Covered

| # | Problem | Folder |
|---|---------|--------|
| 1 | Parking Lot System | [`parking-lot/`](parking-lot/) |

## Common Resources

- [UML Arrows & Relationship Guide](UML-ARROWS-GUIDE.md) — quick reference for reading class diagrams

## How to Run Any Question

```bash
cd <question-folder>
mvn clean package
java -jar target/<artifact>.jar
```

Or compile directly with `javac` — see each subfolder's README for details.
