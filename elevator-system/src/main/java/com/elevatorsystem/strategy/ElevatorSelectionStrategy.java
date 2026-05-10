package com.elevatorsystem.strategy;

import com.elevatorsystem.model.Elevator;
import com.elevatorsystem.model.Request;

import java.util.List;
import java.util.Optional;

public interface ElevatorSelectionStrategy {

    Optional<Elevator> selectElevator(List<Elevator> elevators, Request request);
}
