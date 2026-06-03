package com.snakeandladder.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Game board: size + one Map<start, end> for all snakes & ladders (direction derived at lookup).
public class Board {
    private final int size;
    private final Map<Integer, Integer> jumps;

    public Board(int size, List<BoardEntity> entities) {
        // need at least 2 cells to play
        if (size <= 1) {
            throw new IllegalArgumentException("Board size must be greater than 1.");
        }
        this.size = size;
        this.jumps = new HashMap<>();
        for (BoardEntity entity : entities) {
            // jumps must stay inside the board (cell 0 is pre-start, cell `size` is the win cell)
            if (entity.getStart() < 1 || entity.getStart() >= size
                    || entity.getEnd() < 1 || entity.getEnd() >= size) {
                throw new IllegalArgumentException(
                        "Board entity positions must be within (1, " + (size - 1) + ").");
            }
            // two jumps from the same cell is a data bug — fail fast, don't silently overwrite
            if (jumps.containsKey(entity.getStart())) {
                throw new IllegalArgumentException(
                        "Two snakes/ladders cannot share the same start cell: " + entity.getStart());
            }
            jumps.put(entity.getStart(), entity.getEnd());
        }
    }

    public int getSize() {
        return size;
    }

    // Resolves landed cell → snake tail / ladder top / itself. Called by Game.takeTurn each roll.
    public int getFinalPosition(int position) {
        return jumps.getOrDefault(position, position);
    }
}
