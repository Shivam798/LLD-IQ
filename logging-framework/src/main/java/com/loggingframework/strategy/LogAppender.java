package com.loggingframework.strategy;

import com.loggingframework.model.LogMessage;

/**
 * Strategy interface for log output destinations.
 * Each implementation defines where and how log messages are written
 * (e.g., console, file, database). Appenders hold a pluggable LogFormatter
 * that controls the message format independently of the destination.
 * Implement this interface to add new output targets without modifying existing code.
 */
public interface LogAppender {
    void append(LogMessage logMessage);
    void close();
    LogFormatter getFormatter();
    void setFormatter(LogFormatter formatter);
}
