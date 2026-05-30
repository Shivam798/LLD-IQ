package com.movieticketbookingsystem.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Screen {
    private final String id;
    private final String name;
    private final List<Seat> seats;

    public Screen(String id, String name) {
        this.id = id;
        this.name = name;
        this.seats = new CopyOnWriteArrayList<>();
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Seat> getSeats() {
        return seats;
    }
}
