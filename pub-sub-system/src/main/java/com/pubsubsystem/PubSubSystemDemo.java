package com.pubsubsystem;

import com.pubsubsystem.model.Message;
import com.pubsubsystem.model.PubSubService;
import com.pubsubsystem.model.Publisher;
import com.pubsubsystem.observer.AlertSubscriber;
import com.pubsubsystem.observer.NewsSubscriber;
import com.pubsubsystem.observer.Subscriber;

/**
 * Entry point demonstrating 5 scenarios:
 * 1. Basic pub-sub — create topics, subscribe, publish, receive
 * 2. Multiple subscribers on one topic — fan-out delivery
 * 3. Publisher abstraction — named publishers sending messages
 * 4. Unsubscribe — subscriber stops receiving after unsubscribe
 * 5. Multi-threaded publishing — concurrent publishers on same topic
 */
public class PubSubSystemDemo {

    public static void main(String[] args) throws InterruptedException {
        PubSubService service = PubSubService.getInstance();

        // --- 1. Basic Pub-Sub ---
        System.out.println("=== 1. Basic Pub-Sub ===");
        service.createTopic("SPORTS");
        service.createTopic("TECH");
        service.createTopic("WEATHER");

        Subscriber sportsFan1 = new NewsSubscriber("SportsFan1");
        Subscriber sportsFan2 = new NewsSubscriber("SportsFan2");
        Subscriber techie = new NewsSubscriber("Techie");
        Subscriber allReader = new NewsSubscriber("AllReader");
        Subscriber admin = new AlertSubscriber("SystemAdmin");

        service.subscribe("SPORTS", sportsFan1);
        service.subscribe("SPORTS", sportsFan2);
        service.subscribe("TECH", techie);

        System.out.println("\nPublishing to SPORTS...");
        service.publish("SPORTS", new Message("Team A wins the championship!"));
        Thread.sleep(200);

        System.out.println("\nPublishing to TECH...");
        service.publish("TECH", new Message("New AI model released."));
        Thread.sleep(200);

        // --- 2. Multiple Subscribers (Fan-Out) ---
        System.out.println("\n=== 2. Fan-Out — One Topic, Many Subscribers ===");
        service.subscribe("SPORTS", allReader);
        service.subscribe("SPORTS", admin);
        service.subscribe("TECH", allReader);

        service.publish("SPORTS", new Message("Major player traded to Team B."));
        Thread.sleep(200);

        // --- 3. Publisher Abstraction ---
        System.out.println("\n=== 3. Publisher Abstraction ===");
        Publisher weatherBot = new Publisher("WeatherBot");
        Subscriber weatherWatcher = new NewsSubscriber("WeatherWatcher");
        service.subscribe("WEATHER", weatherWatcher);

        weatherBot.publish("WEATHER", new Message("Sunny with a high of 75F."));
        Thread.sleep(200);

        // --- 4. Unsubscribe ---
        System.out.println("\n=== 4. Unsubscribe ===");
        service.unsubscribe("SPORTS", sportsFan2);

        System.out.println("Publishing after SportsFan2 unsubscribed...");
        service.publish("SPORTS", new Message("Season ticket sales open!"));
        Thread.sleep(200);

        // --- 5. Multi-Threaded Publishing ---
        System.out.println("\n=== 5. Multi-Threaded Publishing ===");
        Publisher publisher1 = new Publisher("FastPublisher-1");
        Publisher publisher2 = new Publisher("FastPublisher-2");

        Thread t1 = new Thread(() -> publisher1.publish("TECH", new Message("Breaking: Quantum chip announced!")),
                "PubThread-1");
        Thread t2 = new Thread(() -> publisher2.publish("TECH", new Message("Update: Open-source LLM released.")),
                "PubThread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        Thread.sleep(300);

        // --- Shutdown ---
        service.shutdown();
    }
}
