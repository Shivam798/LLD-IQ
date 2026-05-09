package com.atm.chain;

abstract class NoteDispenser implements DispenseChain {

    private DispenseChain nextChain;
    private final int noteValue;
    private int numNotes;

    protected NoteDispenser(int noteValue, int numNotes) {
        this.noteValue = noteValue;
        this.numNotes = numNotes;
    }

    @Override
    public void setNextChain(DispenseChain nextChain) {
        this.nextChain = nextChain;
    }

    @Override
    public void dispense(int amount) {
        if (amount >= noteValue) {
            int numToDispense = Math.min(amount / noteValue, numNotes);
            int remaining = amount - (numToDispense * noteValue);

            if (numToDispense > 0) {
                System.out.println("    Dispensing " + numToDispense + " x $" + noteValue + " note(s)");
                numNotes -= numToDispense;
            }

            if (remaining > 0 && nextChain != null) {
                nextChain.dispense(remaining);
            }
        } else if (nextChain != null) {
            nextChain.dispense(amount);
        }
    }

    @Override
    public boolean canDispense(int amount) {
        if (amount < 0) return false;
        if (amount == 0) return true;

        int numToUse = Math.min(amount / noteValue, numNotes);
        int remaining = amount - (numToUse * noteValue);

        if (remaining == 0) return true;
        if (nextChain != null) return nextChain.canDispense(remaining);
        return false;
    }
}
