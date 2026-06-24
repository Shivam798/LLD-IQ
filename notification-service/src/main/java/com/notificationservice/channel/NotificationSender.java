package com.notificationservice.channel;

import com.notificationservice.enums.ChannelType;
import com.notificationservice.model.Notification;

/**
 * Strategy interface: one way of physically delivering a notification.
 *
 * Each concrete sender owns the quirks of ONE channel — SMTP for email,
 * an SMS gateway, APNs/FCM for push. The NotificationService talks only
 * to this abstraction, so adding a channel never touches the service
 * (Open/Closed) and the service never type-checks the sender (Liskov).
 *
 * The interface is deliberately tiny (Interface Segregation):
 *   - send()           : do the delivery, return success/failure.
 *   - getChannelType() : lets the factory index senders by channel.
 *
 * send() returns a boolean rather than void so a wrapper (the retry
 * decorator) can observe failure and react. We return false / throw on
 * transient failure — the decorator turns that into a retry.
 */
public interface NotificationSender {

    /**
     * Attempt to deliver. Returns true on success, false on a failure the
     * caller may choose to retry. Implementations may also throw for
     * unexpected errors — the retry decorator treats both the same way.
     */
    boolean send(Notification notification);

    ChannelType getChannelType();
}
