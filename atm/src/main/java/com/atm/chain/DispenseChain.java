package com.atm.chain;

public interface DispenseChain {

    void setNextChain(DispenseChain nextChain);

    void dispense(int amount);

    boolean canDispense(int amount);
}
