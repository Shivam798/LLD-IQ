package com.pubsubsystem.observer;

import com.pubsubsystem.model.Message;

/**
 * Concrete subscriber that prints received messages in a standard news format.
 * Simulates a typical consumer like a news feed reader or dashboard widget.
 */
public class NewsSubscriber implements Subscriber {
    private final String id;

    public NewsSubscriber(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void onMessage(String topicName, Message message) {
        System.out.printf("  [News - %s] Topic '%s': %s%n", id, topicName, message.getContent());
    }
}
