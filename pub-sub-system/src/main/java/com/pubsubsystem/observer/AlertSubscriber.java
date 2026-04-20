package com.pubsubsystem.observer;

import com.pubsubsystem.model.Message;

/**
 * Concrete subscriber that prints received messages as urgent alerts.
 * Simulates a system admin or monitoring service that needs immediate attention.
 */
public record AlertSubscriber(String id) implements Subscriber {

    @Override
    public void onMessage(String topicName, Message message) {
        System.out.printf("  !!! [ALERT - %s] Topic '%s': %s !!!%n", id, topicName, message.getContent());
    }
}
