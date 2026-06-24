package com.notificationservice.observer;

import com.notificationservice.enums.NotificationStatus;
import com.notificationservice.model.Notification;

/**
 * Writes a log line on every status transition. The simplest possible
 * observer — its whole job is to make the lifecycle visible. In production
 * this would push to a structured logger; here it prints.
 */
public class LoggingObserver implements NotificationObserver {

    @Override
    public void onStatusChange(Notification notification, NotificationStatus newStatus) {
        System.out.println("  [LOG] Notification#" + notification.getId()
                + " -> " + newStatus);
    }
}
