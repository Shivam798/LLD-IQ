package com.atm.state;

import com.atm.enums.OperationType;
import com.atm.model.ATM;

public class AuthenticatedState implements ATMState {

    @Override
    public void insertCard(ATM atm, String cardNumber) {
        System.out.println("    Error: A card is already inserted and session is active.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("    Error: Already authenticated.");
    }

    @Override
    public void selectOperation(ATM atm, OperationType op, int... args) {
        switch (op) {
            case CHECK_BALANCE -> atm.checkBalance();

            case WITHDRAW_CASH -> {
                if (args.length == 0 || args[0] <= 0) {
                    System.out.println("    Error: Invalid withdrawal amount.");
                    break;
                }
                int withdrawAmount = args[0];
                double balance = atm.getBankingService().getBalance(atm.getCurrentCard());

                if (withdrawAmount > balance) {
                    System.out.println("    Error: Insufficient balance.");
                    break;
                }
                System.out.println("    Processing withdrawal of $" + withdrawAmount + "...");
                atm.withdrawCash(withdrawAmount);
            }

            case DEPOSIT_CASH -> {
                if (args.length == 0 || args[0] <= 0) {
                    System.out.println("    Error: Invalid deposit amount.");
                    break;
                }
                int depositAmount = args[0];
                System.out.println("    Processing deposit of $" + depositAmount + "...");
                atm.depositCash(depositAmount);
            }
        }

        System.out.println("    Transaction complete.");
        ejectCard(atm);
    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("    Card ejected. Thank you for using our ATM.");
        atm.setCurrentCard(null);
        atm.changeState(new IdleState());
    }
}
