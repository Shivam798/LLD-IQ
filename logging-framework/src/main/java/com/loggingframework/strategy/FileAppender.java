package com.loggingframework.strategy;

import com.loggingframework.model.LogMessage;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Appender that writes formatted log messages to a file.
 * Opens the file in append mode so logs persist across restarts.
 * The append() method is synchronized to prevent interleaved writes
 * when multiple threads log through the async processor.
 * Must be closed via close() or LogManager.shutdown() to flush and release the file handle.
 */
public class FileAppender implements LogAppender {
    private final FileWriter writer;
    private LogFormatter formatter;

    public FileAppender(String filePath) throws IOException {
        this.formatter = new SimpleTextFormatter();
        this.writer = new FileWriter(filePath, true);
    }

    @Override
    public synchronized void append(LogMessage logMessage) {
        try {
            writer.write(formatter.format(logMessage) + "\n");
            writer.flush(); // Write immediately even if app crash
        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to close log file: " + e.getMessage());
        }
    }

    @Override
    public LogFormatter getFormatter() { return formatter; }

    @Override
    public void setFormatter(LogFormatter formatter) { this.formatter = formatter; }
}
