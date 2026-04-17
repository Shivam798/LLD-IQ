package com.loggingframework.model;

import com.loggingframework.enums.LogLevel;
import com.loggingframework.strategy.LogAppender;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Named logger that filters messages by level and dispatches them to appenders.
 * Loggers form a hierarchy based on dot-separated names (e.g., "com.myapp.db").
 * Each logger inherits its parent's effective level unless explicitly overridden.
 * When additivity is true (default), log messages propagate up to parent appenders
 * (Chain of Responsibility pattern).
 * Package-private constructor — instances are created only via LogManager.getLogger().
 */
public class Logger {
    private final String name;
    private final Logger parent;
    private final List<LogAppender> appenders;
    private LogLevel level;
    private boolean additivity = true;

    Logger(String name, Logger parent) {
        this.name = name;
        this.parent = parent;
        this.appenders = new CopyOnWriteArrayList<>();
    }

    public void addAppender(LogAppender appender) {
        appenders.add(appender);
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public void setAdditivity(boolean additivity) {
        this.additivity = additivity;
    }

    public LogLevel getEffectiveLevel() {
        for (Logger current = this; current != null; current = current.parent) {
            if (current.level != null) {
                return current.level;
            }
        }
        return LogLevel.DEBUG;
    }

    public void log(LogLevel messageLevel, String message) {
        if (messageLevel.isGreaterOrEqual(getEffectiveLevel())) {
            LogMessage logMessage = new LogMessage(messageLevel, this.name, message);
            callAppenders(logMessage);
        }
    }

    private void callAppenders(LogMessage logMessage) {
        if (!appenders.isEmpty()) {
            LogManager.getInstance().getProcessor().process(logMessage, this.appenders);
        }
        if (additivity && parent != null) {
            parent.callAppenders(logMessage);
        }
    }

    public void debug(String message) { log(LogLevel.DEBUG, message); }
    public void info(String message)  { log(LogLevel.INFO, message); }
    public void warn(String message)  { log(LogLevel.WARN, message); }
    public void error(String message) { log(LogLevel.ERROR, message); }
    public void fatal(String message) { log(LogLevel.FATAL, message); }

    List<LogAppender> getAppenders() { return appenders; }
    String getName() { return name; }
}
