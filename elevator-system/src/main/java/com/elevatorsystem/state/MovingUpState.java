package com.elevatorsystem.state;

import com.elevatorsystem.enums.Direction;
import com.elevatorsystem.enums.RequestSource;
import com.elevatorsystem.model.Elevator;
import com.elevatorsystem.model.Request;

public class MovingUpState implements ElevatorState {

    // Called every tick (1s) by the elevator's thread — moves one floor upward
    @Override
    public void move(Elevator elevator) {
        // Nothing left to serve going up — transition to Idle
        if (elevator.getUpRequests().isEmpty()) {
            elevator.setState(new IdleState());
            return;
        }

        // Peek the nearest target above (TreeSet is sorted ascending, first() = lowest = nearest)
        Integer nextFloor = elevator.getUpRequests().first();

        // Move one floor up (LOOK algorithm — sweep upward serving requests along the way)
        elevator.setCurrentFloor(elevator.getCurrentFloor() + 1);

        // Arrived at a requested floor — stop and remove it from the queue
        if (elevator.getCurrentFloor() == nextFloor) {
            System.out.println("Elevator " + elevator.getId() + " stopped at floor " + nextFloor);
            elevator.getUpRequests().pollFirst();
        }

        // All up-requests served — go Idle (will pick up downRequests on next move() cycle)
        if (elevator.getUpRequests().isEmpty()) {
            elevator.setState(new IdleState());
        }
    }

    // Called when a new request arrives while we're moving up
    @Override
    public void addRequest(Elevator elevator, Request request) {
        // INTERNAL request (cabin button) — user is already inside this elevator
        if (request.getSource() == RequestSource.INTERNAL) {
            // Floor is above us → serve on the way up
            if (request.getTargetFloor() > elevator.getCurrentFloor()) {
                elevator.getUpRequests().add(request.getTargetFloor());
            } else {
                // Floor is below us → queue for later when we reverse direction
                elevator.getDownRequests().add(request.getTargetFloor());
            }
            return;
        }

        // EXTERNAL request (hall call) — someone on a floor wants an elevator
        // Pick them up only if they're ahead of us going the same direction (LOOK optimization)
        if (request.getDirection() == Direction.UP
                && request.getTargetFloor() >= elevator.getCurrentFloor()) {
            elevator.getUpRequests().add(request.getTargetFloor());
        } else if (request.getDirection() == Direction.DOWN) {
            // They want to go down — we can't serve them now, queue for the return sweep
            elevator.getDownRequests().add(request.getTargetFloor());
        }
    }

    @Override
    public Direction getDirection() {
        return Direction.UP;
    }
}
