package com.loggingframework.strategy;

import com.loggingframework.model.LogMessage;

/**
 * Strategy interface for formatting log messages into strings.
 * Decouples message formatting from the output destination (appender),
 * so the same appender can produce different formats (plain text, JSON, XML)
 * by swapping the formatter. Implement this to add new output formats.
 */
public interface LogFormatter {
    String format(LogMessage logMessage);
}
