package com.loggingframework.model;

import com.loggingframework.strategy.LogAppender;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Package-private async processor that decouples log emission from log writing.
 * Uses a single-thread ExecutorService so callers (application threads) are never
 * blocked by slow appenders (e.g., file I/O). The daemon thread ensures the JVM
 * can exit even if the processor hasn't been explicitly stopped.
 * On shutdown, it drains pending tasks with a 2-second timeout before forcing stop.
 */
class AsyncLogProcessor {
    private final ExecutorService executor;

    AsyncLogProcessor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "AsyncLogProcessor");
            thread.setDaemon(true);
            return thread;
        });
    }

    void process(LogMessage logMessage, List<LogAppender> appenders) {
        if (executor.isShutdown()) {
            System.err.println("Logger is shut down. Cannot process log message.");
            return;
        }
        executor.submit(() -> {
            for (LogAppender appender : appenders) {
                appender.append(logMessage);
            }
        });
    }

    void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println("Log processor did not terminate in time. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
