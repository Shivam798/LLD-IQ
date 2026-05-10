package com.elevatorsystem.model;

import com.elevatorsystem.enums.Direction;
import com.elevatorsystem.enums.RequestSource;
import com.elevatorsystem.observer.ElevatorDisplay;
import com.elevatorsystem.strategy.ElevatorSelectionStrategy;
import com.elevatorsystem.strategy.NearestElevatorStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ElevatorSystem {

    private static volatile ElevatorSystem instance;

    private final Map<Integer, Elevator> elevators;
    private final ElevatorSelectionStrategy selectionStrategy;
    private final ExecutorService executorService;

    private ElevatorSystem(int numElevators) {
        this.selectionStrategy = new NearestElevatorStrategy();
        this.executorService = Executors.newFixedThreadPool(numElevators);

        ElevatorDisplay display = new ElevatorDisplay();
        List<Elevator> elevatorList = new ArrayList<>();
        for (int i = 1; i <= numElevators; i++) {
            Elevator elevator = new Elevator(i);
            elevator.addObserver(display);
            elevatorList.add(elevator);
        }
        this.elevators = elevatorList.stream()
                .collect(Collectors.toMap(Elevator::getId, e -> e));
    }

    public static ElevatorSystem getInstance(int numElevators) {
        if (instance == null) {
            synchronized (ElevatorSystem.class) {
                if (instance == null) {
                    instance = new ElevatorSystem(numElevators);
                }
            }
        }
        return instance;
    }

    public void start() {
        for (Elevator elevator : elevators.values()) {
            executorService.submit(elevator);
        }
    }

    // ── External request (hall call) ────────────────────────────

    public void requestElevator(int floor, Direction direction) {
        System.out.println("\n>> EXTERNAL Request: User at floor " + floor + " wants to go " + direction);
        Request request = new Request(floor, direction, RequestSource.EXTERNAL);

        Optional<Elevator> selected = selectionStrategy.selectElevator(
                new ArrayList<>(elevators.values()), request);

        if (selected.isPresent()) {
            selected.get().addRequest(request);
        } else {
            System.out.println("System busy, please wait.");
        }
    }

    // ── Internal request (cabin call) ───────────────────────────

    public void selectFloor(int elevatorId, int destinationFloor) {
        System.out.println("\n>> INTERNAL Request: User in Elevator " + elevatorId
                + " selected floor " + destinationFloor);
        Request request = new Request(destinationFloor, Direction.IDLE, RequestSource.INTERNAL);

        Elevator elevator = elevators.get(elevatorId);
        if (elevator != null) {
            elevator.addRequest(request);
        } else {
            System.err.println("Invalid elevator ID.");
        }
    }

    public void shutdown() {
        System.out.println("Shutting down elevator system...");
        for (Elevator elevator : elevators.values()) {
            elevator.stopElevator();
        }
        executorService.shutdown();
    }
}
