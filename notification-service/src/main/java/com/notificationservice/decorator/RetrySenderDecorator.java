package com.notificationservice.decorator;

import com.notificationservice.channel.NotificationSender;
import com.notificationservice.model.Notification;

/**
 * Adds retry-on-failure to ANY sender, without that sender knowing.
 *
 * This is the payoff of the Decorator pattern: retry is a cross-cutting
 * concern that applies equally to email, SMS, and push. Baking a retry
 * loop into each sender would duplicate the logic three times and violate
 * SRP (a sender's job is to send once, not to manage a retry policy).
 * Instead we wrap the sender once and the policy lives in exactly one class.
 *
 * Delivery failures are usually transient (gateway hiccup, momentary
 * network blip), so trying again a moment later often succeeds — which is
 * why retry is the single highest-value reliability wrapper to demonstrate.
 *
 * Note: real production retries add EXPONENTIAL BACKOFF + JITTER between
 * attempts so a downstream outage doesn't get hammered by synchronised
 * retries. We omit the sleep here to keep the demo instant, but call it
 * out — an interviewer will ask.
 */
public class RetrySenderDecorator extends NotificationSenderDecorator {

    private final int maxAttempts;

    public RetrySenderDecorator(NotificationSender delegate, int maxAttempts) {
        super(delegate);
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean send(Notification notification) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (delegate.send(notification)) {
                    if (attempt > 1) {
                        System.out.println("  [RETRY] succeeded on attempt " + attempt);
                    }
                    return true;
                }
                System.out.println("  [RETRY] attempt " + attempt + "/" + maxAttempts
                        + " failed for " + notification);
            } catch (RuntimeException ex) {
                // Treat a thrown error the same as a returned false — both
                // mean "this attempt didn't deliver". The last attempt's
                // exception is swallowed into a FAILED result rather than
                // propagating, because one bad notification shouldn't crash
                // the caller's batch.
                System.out.println("  [RETRY] attempt " + attempt + "/" + maxAttempts
                        + " threw: " + ex.getMessage());
            }
            // (production) sleep with exponential backoff + jitter here.
        }
        return false;
    }
}
