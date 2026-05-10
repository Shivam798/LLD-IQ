package com.elevatorsystem.model;

import com.elevatorsystem.enums.Direction;
import com.elevatorsystem.observer.ElevatorObserver;
import com.elevatorsystem.state.ElevatorState;
import com.elevatorsystem.state.IdleState;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Elevator implements Runnable {

    private final int id;
    private final AtomicInteger currentFloor;
    private ElevatorState state;
    private volatile boolean running = true;

    private final TreeSet<Integer> upRequests;
    private final TreeSet<Integer> downRequests;

    private final List<ElevatorObserver> observers = new ArrayList<>();

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = new AtomicInteger(1);
        this.upRequests = new TreeSet<>();
        this.downRequests = new TreeSet<>((a, b) -> b - a); // descending for down travel
        this.state = new IdleState();
    }

    // ── Observer methods ────────────────────────────────────────

    public void addObserver(ElevatorObserver observer) {
        observers.add(observer);
        observer.update(this);
    }

    private void notifyObservers() {
        for (ElevatorObserver observer : observers) {
            observer.update(this);
        }
    }

    // ── State pattern delegation ────────────────────────────────

    public void setState(ElevatorState newState) {
        this.state = newState;
        notifyObservers();
    }

    public void move() {
        state.move(this);
    }

    public synchronized void addRequest(Request request) {
        System.out.println("Elevator " + id + " processing: " + request);
        state.addRequest(this, request);
    }

    // ── Getters / setters ───────────────────────────────────────

    public int getId() { return id; }

    public int getCurrentFloor() { return currentFloor.get(); }

    public void setCurrentFloor(int floor) {
        this.currentFloor.set(floor);
        notifyObservers();
    }

    public Direction getDirection() { return state.getDirection(); }

    public TreeSet<Integer> getUpRequests() { return upRequests; }

    public TreeSet<Integer> getDownRequests() { return downRequests; }

    public boolean isRunning() { return running; }

    public void stopElevator() { this.running = false; }

    // ── Runnable ────────────────────────────────────────────────

    @Override
    public void run() {
        while (running) {
            move();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
}
