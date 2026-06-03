package com.snakeandladder.model;

public class Ladder extends BoardEntity {

    public Ladder(int bottom, int top) {
        super(bottom, top);
        if (bottom >= top) {
            throw new IllegalArgumentException(
                    "Ladder bottom must be at a lower position than its top.");
        }
    }
}
