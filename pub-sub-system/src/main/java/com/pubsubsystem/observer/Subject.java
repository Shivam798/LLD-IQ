package com.pubsubsystem.observer;

import com.pubsubsystem.model.Message;

/**
 * Subject interface for the Observer pattern.
 * Any entity that wants to maintain a list of observers and notify them
 * of state changes (new messages) must implement this contract.
 *
 * In our pub-sub system, Topic is the concrete subject — it holds
 * subscribers and broadcasts messages to all of them.
 */
public interface Subject {
    void addSubscriber(Subscriber subscriber);
    void removeSubscriber(Subscriber subscriber);
    void broadcast(Message message);
}
