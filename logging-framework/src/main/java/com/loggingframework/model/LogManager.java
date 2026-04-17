package com.loggingframework.model;

import com.loggingframework.strategy.LogAppender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that owns the logger hierarchy and the async log processor.
 * Creates loggers on demand via getLogger(name), automatically building
 * the parent chain from dot-separated names (e.g., "com.myapp" → "com" → "root").
 * Also responsible for graceful shutdown — drains pending logs and closes all appenders.
 */
public class LogManager {
    private static final LogManager INSTANCE = new LogManager();

    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private final Logger rootLogger;
    private final AsyncLogProcessor processor;

    private LogManager() {
        this.rootLogger = new Logger("root", null);
        this.loggers.put("root", rootLogger);
        this.processor = new AsyncLogProcessor();
    }

    public static LogManager getInstance() {
        return INSTANCE;
    }

    public synchronized Logger getLogger(String name) {
        Logger existing = loggers.get(name);
        if (existing != null) {
            return existing;
        }

        int lastDot = name.lastIndexOf('.');
        String parentName = (lastDot == -1) ? "root" : name.substring(0, lastDot);
        Logger parent = getLogger(parentName);
        Logger logger = new Logger(name, parent);
        loggers.put(name, logger);
        return logger;
    }

    public Logger getRootLogger() {
        return rootLogger;
    }

    AsyncLogProcessor getProcessor() {
        return processor;
    }

    public void shutdown() {
        processor.stop();
        loggers.values().stream()
                .flatMap(logger -> logger.getAppenders().stream())
                .distinct()
                .forEach(LogAppender::close);
        System.out.println("Logging framework shut down gracefully.");
    }
}
