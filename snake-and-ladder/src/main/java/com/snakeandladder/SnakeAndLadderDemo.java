package com.snakeandladder;

import com.snakeandladder.model.BoardEntity;
import com.snakeandladder.model.Dice;
import com.snakeandladder.model.Ladder;
import com.snakeandladder.model.Snake;

import java.util.List;

public class SnakeAndLadderDemo {
    public static void main(String[] args) {
        List<BoardEntity> boardEntities = List.of(
                new Snake(17, 7),
                new Snake(54, 34),
                new Snake(62, 19),
                new Snake(98, 79),
                new Ladder(3, 38),
                new Ladder(24, 33),
                new Ladder(42, 93),
                new Ladder(72, 84)
        );

        Game game1 = new Game.Builder()
                .setName("Session-A")
                .setBoard(100, boardEntities)
                .setPlayers(List.of("Alice", "Bob", "Charlie"))
                .setDice(new Dice(1, 6))
                .build();

        Game game2 = new Game.Builder()
                .setName("Session-B")
                .setBoard(100, boardEntities)
                .setPlayers(List.of("Dave", "Eve"))
                .setDice(new Dice(1, 6))
                .build();

        GameManager manager = GameManager.getInstance();
        manager.startGame(game1);
        manager.startGame(game2);
        manager.awaitAll();

        System.out.println("\nAll sessions complete.");
        for (Game g : manager.getActiveGames()) {
            String winner = g.getWinner() == null ? "<none>" : g.getWinner().getName();
            System.out.println("  " + g.getName() + " -> winner: " + winner);
        }
    }
}
