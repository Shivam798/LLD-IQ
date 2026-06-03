package com.snakeandladder.model;

import java.util.concurrent.ThreadLocalRandom;

public class Dice {
    private final int minValue;
    private final int maxValue;

    public Dice(int minValue, int maxValue) {
        if (minValue < 1 || maxValue < minValue) {
            throw new IllegalArgumentException("Invalid dice range.");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int roll() {
        return ThreadLocalRandom.current().nextInt(minValue, maxValue + 1);
    }

    public int getMaxValue() {
        return maxValue;
    }
}
