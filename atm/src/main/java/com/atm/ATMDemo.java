package com.atm;

import com.atm.enums.OperationType;
import com.atm.model.ATM;

public class ATMDemo {

    public static void main(String[] args) {
        ATM atm = ATM.getInstance();

        // ── Scenario 1: Check Balance ──────────────────────────────
        System.out.println("=== Scenario 1: Check Balance ===");
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.CHECK_BALANCE);

        // ── Scenario 2: Withdraw Cash ($570) ───────────────────────
        System.out.println("\n=== Scenario 2: Withdraw $570 ===");
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.WITHDRAW_CASH, 570);

        // ── Scenario 3: Deposit Cash ($200) ────────────────────────
        System.out.println("\n=== Scenario 3: Deposit $200 ===");
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.DEPOSIT_CASH, 200);

        // ── Scenario 4: Verify balance after transactions ──────────
        System.out.println("\n=== Scenario 4: Check Balance after withdraw & deposit ===");
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.CHECK_BALANCE);

        // ── Scenario 5: Withdraw more than balance ─────────────────
        System.out.println("\n=== Scenario 5: Withdraw $700 (insufficient balance) ===");
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.WITHDRAW_CASH, 700);

        // ── Scenario 6: Incorrect PIN ──────────────────────────────
        System.out.println("\n=== Scenario 6: Incorrect PIN ===");
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("9999");

        // ── Scenario 7: Second account ─────────────────────────────
        System.out.println("\n=== Scenario 7: Second account — check balance & withdraw ===");
        atm.insertCard("9876-5432-1098-7654");
        atm.enterPin("4321");
        atm.selectOperation(OperationType.CHECK_BALANCE);

        atm.insertCard("9876-5432-1098-7654");
        atm.enterPin("4321");
        atm.selectOperation(OperationType.WITHDRAW_CASH, 200);
    }
}
