package com.movieticketbookingsystem.strategy.pricing;

import com.movieticketbookingsystem.model.Seat;

import java.util.List;

public interface PricingStrategy {
    double calculatePrice(List<Seat> seats);
}
