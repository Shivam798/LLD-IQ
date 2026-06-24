package com.notificationservice.model;

import com.notificationservice.enums.ChannelType;
import com.notificationservice.enums.NotificationStatus;
import com.notificationservice.enums.Priority;

import java.util.concurrent.atomic.AtomicLong;

/**
 * One notification: a message + where it goes + how it's travelling.
 *
 * Built via the BUILDER pattern. A notification has a handful of required
 * fields (recipient, channel, body) and several optional ones (subject,
 * priority). A telescoping constructor —
 *     new Notification(r, EMAIL, "body", "subject", HIGH)
 * — is unreadable at the call site (what does the 4th arg mean?) and a
 * setter-based POJO can't be made immutable. The builder gives us named,
 * order-independent, validated construction AND a final object.
 *
 * The CONTENT is immutable (final fields). Only `status` mutates, because
 * the lifecycle (PENDING -> SENT/FAILED) genuinely changes over time and
 * the service flips it as delivery progresses. That single mutable field
 * is what observers react to.
 */
public final class Notification {

    // Process-wide monotonic id source. AtomicLong so two threads creating
    // notifications at once can never collide on the same id.
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(1);

    private final long id;
    private final Recipient recipient;
    private final ChannelType channel;
    private final String body;
    //Optional Fields
    private final String subject;
    private final Priority priority;

    // The only mutable field — flipped by NotificationService as the
    // notification moves through its lifecycle. Volatile so a status set
    // by the sending thread is visible to any thread that reads it.
    private volatile NotificationStatus status;

    private Notification(Builder builder) {
        this.id = ID_SEQUENCE.getAndIncrement();
        this.recipient = builder.recipient;
        this.channel = builder.channel;
        this.subject = builder.subject;
        this.body = builder.body;
        this.priority = builder.priority;
        this.status = NotificationStatus.PENDING;
    }

    public long getId() {
        return id;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Priority getPriority() {
        return priority;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Notification#" + id + "[" + channel + " -> " + recipient.getUserId()
                + ", priority=" + priority + ", status=" + status + "]";
    }

    /**
     * Fluent builder. Required fields are taken in the static entry method
     * so they can never be forgotten; optional fields default sensibly.
     */
    public static class Builder {
        private final Recipient recipient;
        private final ChannelType channel;
        private final String body;
        private String subject = "";
        private Priority priority = Priority.NORMAL;

        public Builder(Recipient recipient, ChannelType channel, String body) {
            if (recipient == null) {
                throw new IllegalArgumentException("recipient is required");
            }
            if (channel == null) {
                throw new IllegalArgumentException("channel is required");
            }
            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("body is required");
            }
            this.recipient = recipient;
            this.channel = channel;
            this.body = body;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}
