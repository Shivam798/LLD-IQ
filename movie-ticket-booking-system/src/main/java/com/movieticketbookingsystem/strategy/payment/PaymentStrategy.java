package com.movieticketbookingsystem.strategy.payment;

import com.movieticketbookingsystem.model.Payment;

public interface PaymentStrategy {
    Payment pay(double amount);
}
