package com.elevatorsystem;

import com.elevatorsystem.enums.Direction;
import com.elevatorsystem.model.ElevatorSystem;

public class ElevatorSystemDemo {

    public static void main(String[] args) throws InterruptedException {

        // ── Setup: 2 elevators in the building ─────────────────────
        ElevatorSystem system = ElevatorSystem.getInstance(2);
        system.start();
        System.out.println("Elevator system started. Display observer is active.\n");

        // ── Scenario 1: Hall call — user at floor 5 wants to go UP ─
        System.out.println("=== Scenario 1: External request — floor 5, UP ===");
        system.requestElevator(5, Direction.UP);
        Thread.sleep(100);

        // ── Scenario 2: Cabin call — user inside Elevator 1 presses 10
        System.out.println("\n=== Scenario 2: Internal request — Elevator 1 → floor 10 ===");
        system.selectFloor(1, 10);
        Thread.sleep(200);

        // ── Scenario 3: Hall call — user at floor 3 wants to go DOWN
        System.out.println("\n=== Scenario 3: External request — floor 3, DOWN ===");
        system.requestElevator(3, Direction.DOWN);
        Thread.sleep(300);

        // ── Scenario 4: Cabin call — user inside Elevator 2 presses 1
        System.out.println("\n=== Scenario 4: Internal request — Elevator 2 → floor 1 ===");
        system.selectFloor(2, 1);

        // ── Let simulation run to observe movement ─────────────────
        System.out.println("\n--- Letting simulation run for 1 second ---");
        Thread.sleep(1000);

        // ── Shutdown ───────────────────────────────────────────────
        system.shutdown();
        System.out.println("\n--- SIMULATION END ---");
    }
}
