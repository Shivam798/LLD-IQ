package com.movieticketbookingsystem.strategy.pricing;

import com.movieticketbookingsystem.model.Seat;

import java.util.List;

public class WeekendPricingStrategy implements PricingStrategy {
    private static final double WEEKEND_SURCHARGE = 1.25;

    @Override
    public double calculatePrice(List<Seat> seats) {
        double base = seats.stream()
                .mapToDouble(seat -> seat.getType().getBasePrice())
                .sum();
        return base * WEEKEND_SURCHARGE;
    }
}
