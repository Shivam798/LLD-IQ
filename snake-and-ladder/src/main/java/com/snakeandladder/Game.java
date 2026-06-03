package com.snakeandladder;

import com.snakeandladder.enums.GameStatus;
import com.snakeandladder.model.Board;
import com.snakeandladder.model.BoardEntity;
import com.snakeandladder.model.Dice;
import com.snakeandladder.model.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class Game {
    private final String name;
    private final Board board;
    // Deque used as a circular turn queue: poll the head to play, add to tail to re-queue.
    private final Deque<Player> players;
    private final Dice dice;
    private GameStatus status;
    private Player winner;

    private Game(Builder builder) {
        this.name = builder.name;
        this.board = builder.board;
        this.players = new ArrayDeque<>(builder.players);
        this.dice = builder.dice;
        this.status = GameStatus.NOT_STARTED;
    }

    public void play() {
        // Snake & Ladder is a multi-player game; a single player can't play alone.
        if (players.size() < 2) {
            log("Cannot start. At least 2 players are required.");
            return;
        }

        this.status = GameStatus.RUNNING;
        log("Game started with " + players.size() + " players.");

        // Round-robin loop: pick the next player, let them play, push them back to the end.
        while (status == GameStatus.RUNNING) {
            Player currentPlayer = players.poll();
            takeTurn(currentPlayer);
            // Skip re-queueing the winner — they're done and the loop will exit.
            if (status == GameStatus.RUNNING) {
                players.add(currentPlayer);
            }
        }

        log("Game Finished!");
        if (winner != null) {
            log("Winner: " + winner.getName());
        }
    }

    private void takeTurn(Player player) {
        // Loop because rolling the max value (e.g. 6) earns another roll within the same turn.
        boolean rollAgain = true;
        while (rollAgain && status == GameStatus.RUNNING) {
            int roll = dice.roll();
            log(player.getName() + "'s turn. Rolled " + roll + ".");

            int currentPosition = player.getPosition();
            int nextPosition = currentPosition + roll;

            // Classic rule: you must land exactly on the last cell — overshooting forfeits the turn.
            if (nextPosition > board.getSize()) {
                log(player.getName() + " needs exactly " + board.getSize()
                        + " to win. Turn skipped.");
                return;
            }

            // Exact landing on the final cell — game over, current player wins.
            if (nextPosition == board.getSize()) {
                player.setPosition(nextPosition);
                this.winner = player;
                this.status = GameStatus.FINISHED;
                log(player.getName() + " reached " + board.getSize() + " and won!");
                return;
            }

            // Board resolves snakes/ladders: returns the tail of a snake, top of a ladder, or same cell.
            int finalPosition = board.getFinalPosition(nextPosition);
            if (finalPosition > nextPosition) {
                log(player.getName() + " climbed a ladder from " + nextPosition
                        + " to " + finalPosition + ".");
            } else if (finalPosition < nextPosition) {
                log(player.getName() + " bitten by a snake at " + nextPosition
                        + ", slid to " + finalPosition + ".");
            } else {
                log(player.getName() + " moved from " + currentPosition
                        + " to " + finalPosition + ".");
            }
            player.setPosition(finalPosition);

            // Bonus roll only when the dice shows its max face.
            rollAgain = (roll == dice.getMaxValue());
            if (rollAgain) {
                log(player.getName() + " rolled max and gets another turn!");
            }
        }
    }

    private void log(String message) {
        System.out.println("[" + name + "] " + message);
    }

    public GameStatus getStatus() {
        return status;
    }

    public Player getWinner() {
        return winner;
    }

    public String getName() {
        return name;
    }

    // Builder pattern: Game has many required collaborators (board, players, dice) — the
    // builder lets callers configure them step-by-step with readable, named setters instead
    // of a long positional constructor, and validates required pieces at build() time.
    public static class Builder {
        private String name = "Game";
        private Board board;
        private List<Player> players;
        private Dice dice;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setBoard(int boardSize, List<BoardEntity> boardEntities) {
            this.board = new Board(boardSize, boardEntities);
            return this;
        }

        public Builder setPlayers(List<String> playerNames) {
            // Map each name → new Player via constructor reference (Player::new is shorthand
            // for n -> new Player(n)). Stream API replaces an explicit loop with intent.
            this.players = playerNames.stream()
                    .map(Player::new)
                    .collect(Collectors.toList());
            return this;
        }

        public Builder setDice(Dice dice) {
            this.dice = dice;
            return this;
        }

        public Game build() {
            // Fail fast if the caller forgot to wire a required dependency.
            if (board == null || players == null || dice == null) {
                throw new IllegalStateException("Board, Players, and Dice must be set.");
            }
            return new Game(this);
        }
    }
}
