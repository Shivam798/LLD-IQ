package com.notificationservice.decorator;

import com.notificationservice.channel.NotificationSender;
import com.notificationservice.enums.ChannelType;
import com.notificationservice.model.Notification;

/**
 * Abstract base for DECORATORS that add behaviour around a sender without
 * changing the sender itself.
 *
 * A decorator IS-A NotificationSender and HAS-A NotificationSender — that
 * dual relationship is the whole pattern. Because it implements the same
 * interface it wraps, callers (and the factory) can't tell a decorated
 * sender from a bare one: you can stack RetryDecorator(RateLimitDecorator(
 * EmailSender)) and the service still just calls send().
 *
 * This base forwards every call straight through to the wrapped sender.
 * Concrete decorators override only the method whose behaviour they want
 * to augment (e.g. RetrySenderDecorator overrides send()).
 */
public abstract class NotificationSenderDecorator implements NotificationSender {

    protected final NotificationSender delegate;

    protected NotificationSenderDecorator(NotificationSender delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate sender is required");
        }
        this.delegate = delegate;
    }

    @Override
    public boolean send(Notification notification) {
        return delegate.send(notification);
    }

    @Override
    public ChannelType getChannelType() {
        // A decorator handles the same channel as whatever it wraps — this
        // is what lets a decorated sender be registered back into the
        // factory transparently.
        return delegate.getChannelType();
    }
}
