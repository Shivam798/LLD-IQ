package com.atm.model;

import com.atm.chain.DispenseChain;
import com.atm.chain.NoteDispenser100;
import com.atm.chain.NoteDispenser20;
import com.atm.chain.NoteDispenser50;
import com.atm.enums.OperationType;
import com.atm.state.ATMState;
import com.atm.state.IdleState;

public class ATM {

    private static volatile ATM instance;

    private final BankingService bankingService;
    private final CashDispenser cashDispenser;
    private ATMState currentState;
    private Card currentCard;

    private ATM() {
        this.bankingService = new BankingService();
        this.currentState = new IdleState();

        // Setup the dispenser chain: $100 → $50 → $20
        DispenseChain c1 = new NoteDispenser100(10);  // 10 x $100 notes
        DispenseChain c2 = new NoteDispenser50(20);   // 20 x $50 notes
        DispenseChain c3 = new NoteDispenser20(30);   // 30 x $20 notes
        c1.setNextChain(c2);
        c2.setNextChain(c3);
        this.cashDispenser = new CashDispenser(c1);
    }

    public static ATM getInstance() {
        if (instance == null) {
            synchronized (ATM.class) {
                if (instance == null) {
                    instance = new ATM();
                }
            }
        }
        return instance;
    }

    static void resetInstance() {
        synchronized (ATM.class) {
            instance = null;
        }
    }

    // ── State transitions ──────────────────────────────────────────

    public void changeState(ATMState newState) {
        this.currentState = newState;
    }

    public void setCurrentCard(Card card) {
        this.currentCard = card;
    }

    // ── Delegated to current state ─────────────────────────────────

    public void insertCard(String cardNumber) {
        currentState.insertCard(this, cardNumber);
    }

    public void enterPin(String pin) {
        currentState.enterPin(this, pin);
    }

    public void selectOperation(OperationType op, int... args) {
        currentState.selectOperation(this, op, args);
    }

    // ── Banking operations ─────────────────────────────────────────

    public Card getCard(String cardNumber) {
        return bankingService.getCard(cardNumber);
    }

    public boolean authenticate(String pin) {
        return bankingService.authenticate(currentCard, pin);
    }

    public void checkBalance() {
        double balance = bankingService.getBalance(currentCard);
        System.out.printf("    Your current account balance is: $%.2f%n", balance);
    }

    public void withdrawCash(int amount) {
        if (!cashDispenser.canDispenseCash(amount)) {
            System.out.println("    Error: Insufficient cash available in the ATM.");
            return;
        }

        bankingService.withdrawMoney(currentCard, amount);

        try {
            cashDispenser.dispenseCash(amount);
        } catch (Exception e) {
            // Rollback if dispensing fails
            bankingService.depositMoney(currentCard, amount);
            System.out.println("    Error: Cash dispensing failed. Amount refunded.");
        }
    }

    public void depositCash(int amount) {
        bankingService.depositMoney(currentCard, amount);
        System.out.println("    $" + amount + " deposited successfully.");
    }

    // ── Accessors needed by states ─────────────────────────────────

    public Card getCurrentCard() {
        return currentCard;
    }

    public BankingService getBankingService() {
        return bankingService;
    }
}
