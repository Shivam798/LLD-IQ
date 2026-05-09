package com.atm.model;

public class Card {

    private final String cardNumber;
    private final String pin;

    public Card(String cardNumber, String pin) {
        this.cardNumber = cardNumber;
        this.pin = pin;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getPin() {
        return pin;
    }
}

//This class can be a record
//public record Card(String cardNumber, String pin) { }
