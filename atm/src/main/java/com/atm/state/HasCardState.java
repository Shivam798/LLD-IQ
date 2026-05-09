package com.atm.state;

import com.atm.enums.OperationType;
import com.atm.model.ATM;

public class HasCardState implements ATMState {

    @Override
    public void insertCard(ATM atm, String cardNumber) {
        System.out.println("    Error: A card is already inserted.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("    Authenticating PIN...");
        boolean authenticated = atm.authenticate(pin);

        if (authenticated) {
            System.out.println("    Authentication successful.");
            atm.changeState(new AuthenticatedState());
        } else {
            System.out.println("    Authentication failed: Incorrect PIN.");
            ejectCard(atm);
        }
    }

    @Override
    public void selectOperation(ATM atm, OperationType op, int... args) {
        System.out.println("    Error: Please enter your PIN first.");
    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("    Card ejected.");
        atm.setCurrentCard(null);
        atm.changeState(new IdleState());
    }
}
