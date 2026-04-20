package com.pubsubsystem.model;

import com.pubsubsystem.observer.Subject;
import com.pubsubsystem.observer.Subscriber;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

/**
 * Concrete Subject in the Observer pattern.
 * Represents a named channel that subscribers can listen to.
 * Maintains a thread-safe set of subscribers using CopyOnWriteArraySet
 * (optimized for frequent reads with rare subscribe/unsubscribe writes).
 * On broadcast, each subscriber is notified asynchronously via the shared
 * ExecutorService to avoid blocking the publisher thread.
 */
public class Topic implements Subject {
    private final String name;
    private final Set<Subscriber> subscribers;
    private final ExecutorService deliveryExecutor;

    Topic(String name, ExecutorService deliveryExecutor) {
        this.name = name;
        this.subscribers = new CopyOnWriteArraySet<>();
        this.deliveryExecutor = deliveryExecutor;
    }

    public String getName() {
        return name;
    }

    @Override
    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeSubscriber(Subscriber subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void broadcast(Message message) {
        for (Subscriber subscriber : subscribers) {
            deliveryExecutor.submit(() -> {
                try {
                    subscriber.onMessage(this.name, message);
                } catch (Exception e) {
                    System.err.println("Error delivering to subscriber '"
                            + subscriber.id() + "': " + e.getMessage());
                }
            });
        }
    }

    Set<Subscriber> getSubscribers() {
        return subscribers;
    }
}
