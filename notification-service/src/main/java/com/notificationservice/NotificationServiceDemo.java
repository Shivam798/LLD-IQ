package com.notificationservice;

import com.notificationservice.channel.NotificationSender;
import com.notificationservice.decorator.RetrySenderDecorator;
import com.notificationservice.enums.ChannelType;
import com.notificationservice.enums.NotificationStatus;
import com.notificationservice.enums.Priority;
import com.notificationservice.model.Notification;
import com.notificationservice.model.NotificationService;
import com.notificationservice.model.Recipient;
import com.notificationservice.observer.LoggingObserver;
import com.notificationservice.observer.MetricsObserver;

/**
 * Runnable walkthrough of the notification service.
 *
 * Scenarios demonstrated:
 *   1. Send the same message across three channels (Strategy + Factory).
 *   2. Observers logging + metering every status change (Observer).
 *   3. A flaky channel wrapped in RetrySenderDecorator that fails twice
 *      then succeeds (Decorator).
 *   4. A genuine failure (no phone number) ending in FAILED status.
 */
public class NotificationServiceDemo {

    public static void main(String[] args) {
        NotificationService service = NotificationService.getInstance();

        // --- Attach observers (logging + metrics) ---------------------------
        LoggingObserver logger = new LoggingObserver();
        MetricsObserver metrics = new MetricsObserver();
        service.addObserver(logger);
        service.addObserver(metrics);

        // --- A recipient reachable on all three channels --------------------
        Recipient alice = new Recipient(
                "u-alice", "Alice",
                "alice@example.com", "+1-555-0100", "device-token-abc");

        System.out.println("=== 1. Same message, three channels ===");
        for (ChannelType channel : ChannelType.values()) {
            Notification n = new Notification.Builder(alice, channel,
                    "Your order #4821 has shipped!")
                    .subject("Order shipped")
                    .priority(Priority.HIGH)
                    .build();
            service.send(n);
        }

        // --- 2. Retry decorator around a flaky push sender ------------------
        System.out.println("\n=== 2. Flaky channel + retry decorator ===");
        // A sender that fails its first two attempts, then succeeds — stands
        // in for a transient gateway error. Wrapped so retry is transparent.
        NotificationSender flaky = new NotificationSender() {
            private int attempts = 0;

            @Override
            public boolean send(Notification notification) {
                attempts++;
                if (attempts < 3) {
                    return false; // transient failure
                }
                System.out.println("  [PUSH] delivered after transient failures");
                return true;
            }

            @Override
            public ChannelType getChannelType() {
                return ChannelType.PUSH;
            }
        };
        // Override the PUSH sender with a retry-wrapped flaky one. The
        // service is none the wiser — it still just calls send().
        service.getSenderFactory().register(new RetrySenderDecorator(flaky, 5));

        Notification pushAgain = new Notification.Builder(alice, ChannelType.PUSH,
                "Flash sale ends in 1 hour")
                .subject("Flash sale")
                .priority(Priority.URGENT)
                .build();
        boolean ok = service.send(pushAgain);
        System.out.println("  delivered=" + ok);

        // --- 3. A genuine failure: recipient has no phone number ------------
        System.out.println("\n=== 3. Unreachable channel -> FAILED ===");
        Recipient bob = new Recipient("u-bob", "Bob",
                "bob@example.com", null /* no phone */, null);
        Notification smsToBob = new Notification.Builder(bob, ChannelType.SMS,
                "This will fail — Bob has no phone on file")
                .build();
        service.send(smsToBob);

        // --- 4. Metrics summary --------------------------------------------
        System.out.println("\n=== Metrics ===");
        System.out.println("  SENT   = " + metrics.getCount(NotificationStatus.SENT));
        System.out.println("  FAILED = " + metrics.getCount(NotificationStatus.FAILED));
    }
}
