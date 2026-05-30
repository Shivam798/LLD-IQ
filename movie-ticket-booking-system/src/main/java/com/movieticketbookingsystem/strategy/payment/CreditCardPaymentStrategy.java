package com.movieticketbookingsystem.strategy.payment;

import com.movieticketbookingsystem.enums.PaymentStatus;
import com.movieticketbookingsystem.model.Payment;

import java.util.UUID;

public class CreditCardPaymentStrategy implements PaymentStrategy {
    private final String cardNumber;
    private final String cvv;

    public CreditCardPaymentStrategy(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    @Override
    public Payment pay(double amount) {
        System.out.printf("Processing credit-card payment of ₹%.2f on card ****%s%n",
                amount, cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
        boolean success = Math.random() > 0.05;
        return new Payment(
                amount,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILURE,
                "CC_" + UUID.randomUUID()
        );
    }
}
