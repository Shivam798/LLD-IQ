package com.pubsubsystem.model;

/**
 * Represents a named entity that publishes messages to the pub-sub system.
 * A publisher is bound to the PubSubService and can publish to any topic by name.
 * This is a thin wrapper that provides a clean API — the actual routing and
 * delivery is handled by PubSubService and Topic.
 */
public class Publisher {
    private final String id;
    private final PubSubService pubSubService;

    public Publisher(String id) {
        this.id = id;
        this.pubSubService = PubSubService.getInstance();
    }

    public void publish(String topicName, Message message) {
        System.out.println("Publisher '" + id + "' publishing to topic: " + topicName);
        pubSubService.publish(topicName, message);
    }

    public String getId() {
        return id;
    }
}
