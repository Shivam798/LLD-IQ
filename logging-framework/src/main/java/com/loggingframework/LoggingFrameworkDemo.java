package com.loggingframework;

import com.loggingframework.enums.LogLevel;
import com.loggingframework.model.LogManager;
import com.loggingframework.model.Logger;
import com.loggingframework.strategy.ConsoleAppender;
import com.loggingframework.strategy.FileAppender;

import java.io.IOException;

/**
 * Entry point demonstrating 5 scenarios:
 * 1. Basic logging with level filtering (DEBUG filtered when root=INFO)
 * 2. Logger hierarchy — child loggers inherit parent level, can override
 * 3. File appender with additivity=false (writes only to file, not console)
 * 4. Dynamic level change at runtime (root switched from INFO to DEBUG)
 * 5. Multi-threaded logging — two worker threads log concurrently
 */
public class LoggingFrameworkDemo {

    public static void main(String[] args) throws InterruptedException {
        LogManager logManager = LogManager.getInstance();
        Logger rootLogger = logManager.getRootLogger();
        rootLogger.setLevel(LogLevel.INFO);
        rootLogger.addAppender(new ConsoleAppender());

        // --- 1. Basic Logging ---
        System.out.println("=== Basic Logging ===");
        Logger appLogger = logManager.getLogger("com.myapp.Main");
        appLogger.info("Application starting up.");
        appLogger.debug("This DEBUG message should NOT appear (root level = INFO).");
        appLogger.warn("Low disk space warning.");
        appLogger.error("Failed to connect to database.");

        Thread.sleep(200);

        // --- 2. Logger Hierarchy ---
        System.out.println("\n=== Logger Hierarchy ===");
        Logger dbLogger = logManager.getLogger("com.myapp.db");
        dbLogger.info("Database connection pool initializing.");

        Logger serviceLogger = logManager.getLogger("com.myapp.service.UserService");
        serviceLogger.setLevel(LogLevel.DEBUG);
        serviceLogger.info("User service started.");
        serviceLogger.debug("This DEBUG message SHOULD appear (service level = DEBUG).");

        Thread.sleep(200);

        // --- 3. File Appender ---
        System.out.println("\n=== File Appender ===");
        try {
            FileAppender fileAppender = new FileAppender("application.log");
            Logger fileLogger = logManager.getLogger("com.myapp.file");
            fileLogger.addAppender(fileAppender);
            fileLogger.setAdditivity(false);
            fileLogger.info("This message goes ONLY to the file (additivity=false).");
            System.out.println("  (check application.log for the file-only message)");
        } catch (IOException e) {
            System.err.println("Could not create file appender: " + e.getMessage());
        }

        Thread.sleep(200);

        // --- 4. Dynamic Level Change ---
        System.out.println("\n=== Dynamic Level Change ===");
        System.out.println("  Changing root level to DEBUG...");
        rootLogger.setLevel(LogLevel.DEBUG);
        appLogger.debug("This DEBUG message is now visible after level change.");

        Thread.sleep(200);

        // --- 5. Multi-threaded Logging ---
        System.out.println("\n=== Multi-threaded Logging ===");
        Logger threadLogger = logManager.getLogger("com.myapp.worker");
        Thread t1 = new Thread(() -> threadLogger.info("Log from worker thread 1"), "Worker-1");
        Thread t2 = new Thread(() -> threadLogger.info("Log from worker thread 2"), "Worker-2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        Thread.sleep(300);

        // --- Shutdown ---
        logManager.shutdown();
    }
}
