package com.notificationservice.channel;

import com.notificationservice.enums.ChannelType;
import com.notificationservice.model.Notification;

/**
 * Delivers over SMS. Reads the recipient's phoneNumber. SMS has no
 * "subject" concept, so we send only the body — a good example of why
 * each channel is its own class: the formatting rules differ per channel
 * and don't belong in a single god-sender full of if/else on channel type.
 */
public class SmsSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        String to = notification.getRecipient().getPhoneNumber().orElse(null);
        if (to == null) {
            System.out.println("  [SMS] no phone number for "
                    + notification.getRecipient().getUserId() + " — cannot send");
            return false;
        }
        System.out.println("  [SMS] to=" + to + " | text=\"" + notification.getBody() + "\"");
        return true;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }
}
