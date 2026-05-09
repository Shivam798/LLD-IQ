package com.atm.state;

import com.atm.enums.OperationType;
import com.atm.model.ATM;

public interface ATMState {

    void insertCard(ATM atm, String cardNumber);

    void enterPin(ATM atm, String pin);

    void selectOperation(ATM atm, OperationType op, int... args);

    void ejectCard(ATM atm);
}
