package com.atm.state;

import com.atm.enums.OperationType;
import com.atm.model.ATM;
import com.atm.model.Card;

public class IdleState implements ATMState {

    @Override
    public void insertCard(ATM atm, String cardNumber) {
        System.out.println("\n--- Card inserted ---");
        Card card = atm.getCard(cardNumber);

        if (card == null) {
            System.out.println("    Error: Card not recognized.");
            ejectCard(atm);
        } else {
            atm.setCurrentCard(card);
            System.out.println("    Card accepted. Please enter your PIN.");
            atm.changeState(new HasCardState());
        }
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("    Error: Please insert a card first.");
    }

    @Override
    public void selectOperation(ATM atm, OperationType op, int... args) {
        System.out.println("    Error: Please insert a card first.");
    }

    @Override
    public void ejectCard(ATM atm) {
        atm.setCurrentCard(null);
    }
}
