package com.pubsubsystem.observer;

import com.pubsubsystem.model.Message;

/**
 * Observer interface for the pub-sub system.
 * Any class that wants to receive messages from topics must implement this.
 * Each subscriber has a unique ID for identification and an onMessage callback
 * that is invoked asynchronously when a message is published to a subscribed topic.
 */
public interface Subscriber {
    String id();
    void onMessage(String topicName, Message message);
}
