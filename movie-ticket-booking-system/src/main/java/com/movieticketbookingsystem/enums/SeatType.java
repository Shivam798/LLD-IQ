package com.movieticketbookingsystem.enums;

public enum SeatType {
    REGULAR(150.0),
    PREMIUM(250.0),
    RECLINER(400.0);

    private final double basePrice;

    SeatType(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
