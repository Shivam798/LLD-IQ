package com.notificationservice.observer;

import com.notificationservice.enums.NotificationStatus;
import com.notificationservice.model.Notification;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts how many notifications reached each terminal status.
 *
 * Demonstrates why Observer is more than just logging: this observer keeps
 * its OWN state (per-status counters) that the service neither owns nor
 * knows about. A dashboard could read getCount(SENT) / getCount(FAILED)
 * to compute a delivery success rate.
 *
 * Counters are an AtomicLong per status, held in a ConcurrentHashMap,
 * because notifications may be sent from multiple threads and the
 * increments must not race.
 */
public class MetricsObserver implements NotificationObserver {

    private final ConcurrentHashMap<NotificationStatus, AtomicLong> counts = new ConcurrentHashMap<>();

    @Override
    public void onStatusChange(Notification notification, NotificationStatus newStatus) {
        counts.computeIfAbsent(newStatus, k -> new AtomicLong()).incrementAndGet();
    }

    public long getCount(NotificationStatus status) {
        AtomicLong c = counts.get(status);
        return c == null ? 0L : c.get();
    }
}
