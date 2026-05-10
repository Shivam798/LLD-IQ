package com.elevatorsystem.observer;

import com.elevatorsystem.model.Elevator;

public class ElevatorDisplay implements ElevatorObserver {

    @Override
    public void update(Elevator elevator) {
        System.out.println("[DISPLAY] Elevator " + elevator.getId()
                + " | Floor: " + elevator.getCurrentFloor()
                + " | Direction: " + elevator.getDirection());
    }
}
