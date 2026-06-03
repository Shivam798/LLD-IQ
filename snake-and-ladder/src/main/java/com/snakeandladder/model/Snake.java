package com.snakeandladder.model;

public class Snake extends BoardEntity {

    public Snake(int head, int tail) {
        super(head, tail);
        if (head <= tail) {
            throw new IllegalArgumentException(
                    "Snake head must be at a higher position than its tail.");
        }
    }
}
