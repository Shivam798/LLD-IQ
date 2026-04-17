package com.loggingframework.enums;

/**
 * Defines log severity levels ordered by increasing severity.
 * Used to filter messages — a logger only processes messages
 * whose level is >= the logger's configured effective level.
 */
public enum LogLevel {
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public boolean isGreaterOrEqual(LogLevel other) {
        return this.severity >= other.severity;
    }
}
