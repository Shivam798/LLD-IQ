package com.movieticketbookingsystem.strategy.pricing;

import com.movieticketbookingsystem.model.Seat;

import java.util.List;

public class WeekdayPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(List<Seat> seats) {
        return seats.stream()
                .mapToDouble(seat -> seat.getType().getBasePrice())
                .sum();
    }
}
