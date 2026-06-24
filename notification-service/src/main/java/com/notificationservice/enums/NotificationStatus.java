package com.notificationservice.enums;

/**
 * Lifecycle state of a single notification.
 *
 *   PENDING  -> just created, not yet handed to a sender
 *   SENT     -> a channel sender reported success
 *   FAILED   -> all attempts (including retries) exhausted without success
 *
 * Observers are notified on every transition, which is what lets logging
 * and metrics live OUTSIDE the service (Observer pattern). The status is
 * the single source of truth other components react to.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
