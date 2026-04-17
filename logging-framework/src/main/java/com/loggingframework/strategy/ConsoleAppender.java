package com.loggingframework.strategy;

import com.loggingframework.model.LogMessage;

/**
 * Appender that writes formatted log messages to standard output (System.out).
 * Uses SimpleTextFormatter by default; formatter can be swapped at runtime.
 * No resources to release, so close() is a no-op.
 */
public class ConsoleAppender implements LogAppender {
    private LogFormatter formatter;

    public ConsoleAppender() {
        this.formatter = new SimpleTextFormatter();
    }

    @Override
    public void append(LogMessage logMessage) {
        System.out.println(formatter.format(logMessage));
    }

    @Override
    public void close() {
        // Nothing to close for console output
    }

    @Override
    public LogFormatter getFormatter() { return formatter; }

    @Override
    public void setFormatter(LogFormatter formatter) { this.formatter = formatter; }
}
