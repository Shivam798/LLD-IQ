package com.loggingframework.model;

import com.loggingframework.enums.LogLevel;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Immutable value object representing a single log event.
 * Captures timestamp, severity level, originating logger name,
 * the thread that created the message, and the message content.
 * Thread-safe by design — all fields are final and set at construction time.
 */
@Getter
public final class LogMessage {
    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String threadName;
    private final String message;

    public LogMessage(LogLevel level, String loggerName, String message) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.loggerName = loggerName;
        this.threadName = Thread.currentThread().getName();
        this.message = message;
    }
}
