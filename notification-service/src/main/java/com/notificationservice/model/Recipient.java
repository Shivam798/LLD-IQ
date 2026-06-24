package com.notificationservice.model;

import java.util.Optional;

/**
 * Who a notification is addressed to.
 *
 * A recipient carries every address we might route to — email, phone,
 * device token — because the SAME person can be reached on multiple
 * channels. Which field a sender uses depends on the channel: the
 * EmailSender reads emailAddress, the SmsSender reads phoneNumber, etc.
 *
 * The fields are nullable on purpose (not everyone has a device token),
 * so we expose them as Optional rather than handing back a raw null and
 * inviting a NullPointerException at the call site.
 */
public final class Recipient {

    private final String userId;
    private final String name;
    private final String emailAddress;
    private final String phoneNumber;
    private final String deviceToken;

    public Recipient(String userId,
                     String name,
                     String emailAddress,
                     String phoneNumber,
                     String deviceToken) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        this.userId = userId;
        this.name = name;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.deviceToken = deviceToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getEmailAddress() {
        return Optional.ofNullable(emailAddress);
    }

    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public Optional<String> getDeviceToken() {
        return Optional.ofNullable(deviceToken);
    }
}
