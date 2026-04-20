package com.pubsubsystem.model;

import com.pubsubsystem.observer.Subscriber;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Singleton that manages the entire pub-sub system.
 * Owns the topic registry (ConcurrentHashMap for thread-safe lookups) and
 * a shared cached thread pool for asynchronous message delivery.
 *
 * Responsibilities:
 * - Create and store topics
 * - Subscribe/unsubscribe observers to topics
 * - Route published messages to the correct topic for broadcast
 * - Graceful shutdown — drains pending deliveries before stopping
 *
 * Uses eager initialization (static final) — class loading guarantees thread safety.
 */
public class PubSubService {
    private static final PubSubService INSTANCE = new PubSubService();

    private final Map<String, Topic> topicRegistry;
    private final ExecutorService deliveryExecutor;

    private PubSubService() {
        this.topicRegistry = new ConcurrentHashMap<>();
        this.deliveryExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "PubSub-Delivery");
            thread.setDaemon(true); //Even if application close JVm still process the event which are present in query for publishing
            return thread;
        });
    }

    public static PubSubService getInstance() {
        return INSTANCE;
    }

    public void createTopic(String topicName) {
        topicRegistry.putIfAbsent(topicName, new Topic(topicName, deliveryExecutor));
        System.out.println("Topic created: " + topicName);
    }

    public void subscribe(String topicName, Subscriber subscriber) {
        Topic topic = getTopicOrThrow(topicName);
        topic.addSubscriber(subscriber);
        System.out.println("Subscriber '" + subscriber.id() + "' subscribed to: " + topicName);
    }

    public void unsubscribe(String topicName, Subscriber subscriber) {
        Topic topic = getTopicOrThrow(topicName);
        topic.removeSubscriber(subscriber);
        System.out.println("Subscriber '" + subscriber.id() + "' unsubscribed from: " + topicName);
    }

    public void publish(String topicName, Message message) {
        Topic topic = getTopicOrThrow(topicName);
        System.out.println("Publishing to '" + topicName + "': " + message.getContent());
        topic.broadcast(message);
    }

    public void shutdown() {
        System.out.println("PubSubService shutting down...");

        // Step 1: Stop accepting new tasks, but let already-submitted deliveries finish
        deliveryExecutor.shutdown();

        try {
            // Step 2: Block the current thread for up to 2 seconds, waiting for
            // in-flight deliveries to complete. Returns true if all tasks finished
            // within the timeout, false if some are still running.
            if (!deliveryExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                // Step 3: Timeout expired — some deliveries are still stuck.
                // Force-cancel them via Thread.interrupt() on worker threads.
                System.err.println("Delivery executor did not terminate in time. Forcing shutdown.");
                deliveryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // If *this* thread gets interrupted while waiting, force-stop immediately
            deliveryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("PubSubService shut down gracefully.");
    }

    private Topic getTopicOrThrow(String topicName) {
        Topic topic = topicRegistry.get(topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic not found: " + topicName);
        }
        return topic;
    }
}
