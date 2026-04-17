package com.loggingframework.strategy;

import com.loggingframework.model.LogMessage;

import java.time.format.DateTimeFormatter;

/**
 * Default formatter that produces human-readable single-line log output.
 * Format: "2026-04-12 12:20:57.375 [main] INFO - com.myapp.Main: Application started"
 * Components: timestamp [thread-name] LEVEL - logger-name: message
 */
public class SimpleTextFormatter implements LogFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogMessage logMessage) {
        return String.format("%s [%s] %s - %s: %s",
                logMessage.getTimestamp().format(DATE_TIME_FORMATTER),
                logMessage.getThreadName(),
                logMessage.getLevel(),
                logMessage.getLoggerName(),
                logMessage.getMessage());
    }
}
