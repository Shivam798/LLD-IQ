package com.snakeandladder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Singleton that owns the lifecycle of every Game instance — one manager process-wide,
// so any caller can ask "what games are running?" without passing references around.
public class GameManager {
    // volatile: ensures other threads see the fully-constructed instance, not a half-built one.
    private static volatile GameManager instance;

    // CopyOnWriteArrayList: safe to iterate while another thread mutates (used for getActiveGames).
    private final List<Game> activeGames;
    private final List<Thread> gameThreads;

    private GameManager() {
        this.activeGames = new CopyOnWriteArrayList<>();
        this.gameThreads = new ArrayList<>();
    }

    // Double-checked locking: cheap unlocked read on the hot path, lock only on first init.
    public static GameManager getInstance() {
        if (instance == null) {
            synchronized (GameManager.class) {
                if (instance == null) {
                    instance = new GameManager();
                }
            }
        }
        return instance;
    }

    // synchronized: two callers starting games concurrently must not corrupt the thread list.
    public synchronized void startGame(Game game) {
        activeGames.add(game);
        // Each Game runs on its own thread → multiple games can run in parallel.
        Thread thread = new Thread(game::play, "game-" + game.getName());
        gameThreads.add(thread);
        thread.start();
    }

    // Block the caller until every started game finishes — useful so main() doesn't exit early.
    public void awaitAll() {
        for (Thread t : gameThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // Preserve the interrupt flag so upstream code can react to the interruption.
                Thread.currentThread().interrupt();
            }
        }
    }

    // Defensive copy: callers can't mutate the internal list through this view.
    public List<Game> getActiveGames() {
        return List.copyOf(activeGames);
    }
}
