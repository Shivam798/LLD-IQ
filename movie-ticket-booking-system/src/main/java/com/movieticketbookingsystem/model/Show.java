package com.movieticketbookingsystem.model;

import com.movieticketbookingsystem.strategy.pricing.PricingStrategy;

import java.time.LocalDateTime;

public class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final Cinema cinema;
    private final LocalDateTime startTime;
    private final PricingStrategy pricingStrategy;

    public Show(String id, Movie movie, Screen screen, Cinema cinema, LocalDateTime startTime, PricingStrategy pricingStrategy) {
        this.id = id;
        this.movie = movie;
        this.screen = screen;
        this.cinema = cinema;
        this.startTime = startTime;
        this.pricingStrategy = pricingStrategy;
    }

    public String getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Screen getScreen() {
        return screen;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public PricingStrategy getPricingStrategy() {
        return pricingStrategy;
    }
}
