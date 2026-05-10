package com.elevatorsystem.state;

import com.elevatorsystem.enums.Direction;
import com.elevatorsystem.model.Elevator;
import com.elevatorsystem.model.Request;

public interface ElevatorState {

    void move(Elevator elevator);

    void addRequest(Elevator elevator, Request request);

    Direction getDirection();
}
