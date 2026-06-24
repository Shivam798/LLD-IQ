package com.notificationservice.model;

import com.notificationservice.channel.NotificationSender;
import com.notificationservice.enums.NotificationStatus;
import com.notificationservice.factory.NotificationSenderFactory;
import com.notificationservice.observer.NotificationObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The orchestrator — the one public entry point for sending notifications.
 *
 * SINGLETON: a notification service is a system-wide facility (it owns the
 * sender factory and the observer registry). We want exactly one, reachable
 * from anywhere, so we use the double-checked-locking singleton with a
 * `volatile` instance field.
 *
 * What it actually does (and deliberately nothing more):
 *   1. mark the notification PENDING and tell observers;
 *   2. ask the factory for the sender that handles this channel;
 *   3. delegate the send (the sender may itself be a retry decorator);
 *   4. mark SENT or FAILED and tell observers.
 *
 * Notice what it does NOT do: it has no idea how email differs from SMS
 * (that's the senders), no retry loop (that's a decorator), no logging or
 * metrics (those are observers). It's a thin router — grep it for "smtp"
 * or "retry" and you'll find nothing. That emptiness is the design working.
 */
public class NotificationService {

    // volatile so the half-built instance can't be seen by another thread
    // during double-checked locking.
    private static volatile NotificationService instance;

    private final NotificationSenderFactory senderFactory;

    // CopyOnWriteArrayList: observers are registered rarely (usually once at
    // startup) but iterated on every single send. COW gives lock-free reads
    // and pays the copy cost only on the rare write — exactly this profile.
    private final List<NotificationObserver> observers = new CopyOnWriteArrayList<>();

    private NotificationService() {
        this.senderFactory = new NotificationSenderFactory();
    }

    public static NotificationService getInstance() {
        if (instance == null) {                       // 1st check — no lock on the hot path
            synchronized (NotificationService.class) {
                if (instance == null) {               // 2nd check — under lock
                    instance = new NotificationService();
                }
            }
        }
        return instance;
    }

    public void addObserver(NotificationObserver observer) {
        observers.add(observer);
    }

    public NotificationSenderFactory getSenderFactory() {
        return senderFactory;
    }

    /**
     * Send one notification over its chosen channel. Returns true if the
     * underlying sender (possibly retry-wrapped) reported success.
     */
    public boolean send(Notification notification) {
        updateStatus(notification, NotificationStatus.PENDING);

        NotificationSender sender = senderFactory.getSender(notification.getChannel());
        boolean delivered = sender.send(notification);

        updateStatus(notification,
                delivered ? NotificationStatus.SENT : NotificationStatus.FAILED);
        return delivered;
    }

    /**
     * Flip the status and fan the change out to every observer. Centralised
     * so there's exactly one place that mutates status and notifies — no
     * way for a code path to change status without observers hearing about it.
     */
    private void updateStatus(Notification notification, NotificationStatus status) {
        notification.setStatus(status);
        for (NotificationObserver observer : observers) {
            observer.onStatusChange(notification, status);
        }
    }
}
