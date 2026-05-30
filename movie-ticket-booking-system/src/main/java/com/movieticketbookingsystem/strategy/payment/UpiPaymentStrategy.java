package com.movieticketbookingsystem.strategy.payment;

import com.movieticketbookingsystem.enums.PaymentStatus;
import com.movieticketbookingsystem.model.Payment;

import java.util.UUID;

public class UpiPaymentStrategy implements PaymentStrategy {
    private final String upiId;

    public UpiPaymentStrategy(String upiId) {
        this.upiId = upiId;
    }

    @Override
    public Payment pay(double amount) {
        System.out.printf("Processing UPI payment of ₹%.2f to %s%n", amount, upiId);
        boolean success = Math.random() > 0.05;
        return new Payment(
                amount,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILURE,
                "UPI_" + UUID.randomUUID()
        );
    }
}
