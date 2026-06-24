package com.notificationservice.observer;

import com.notificationservice.enums.NotificationStatus;
import com.notificationservice.model.Notification;

/**
 * Observer interface: react to a notification's status change.
 *
 * The service is responsible for DELIVERING notifications, not for logging
 * them or counting them. Those are separate concerns owned by separate
 * classes (SRP). The Observer pattern lets us attach any number of such
 * concerns — logging, metrics, audit, dead-letter capture — without the
 * service knowing they exist. New cross-cutting reaction = new observer,
 * zero edits to the service (Open/Closed).
 */
public interface NotificationObserver {

    void onStatusChange(Notification notification, NotificationStatus newStatus);
}
