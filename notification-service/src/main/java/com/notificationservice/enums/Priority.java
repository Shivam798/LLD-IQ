package com.notificationservice.enums;

/**
 * Delivery priority of a notification.
 *
 * Kept as an enum (not an int) so callers can't pass a meaningless value
 * like 999. In a fuller system this would drive queue ordering — URGENT
 * jumps ahead of LOW — but even as a plain tag it documents intent and
 * gives observers something to meter on.
 */
public enum Priority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
