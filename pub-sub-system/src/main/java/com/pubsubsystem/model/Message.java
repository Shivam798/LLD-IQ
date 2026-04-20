package com.pubsubsystem.model;

import java.time.LocalDateTime;

/**
 * Immutable value object representing a message in the pub-sub system.
 * Captures the payload content and the timestamp when the message was created.
 * Thread-safe by design — all fields are final, no mutable state.
 */
public final class Message {
    private final String content;
    private final LocalDateTime timestamp;

    public Message(String content) {
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{content='" + content + "', timestamp=" + timestamp + "}";
    }
}
