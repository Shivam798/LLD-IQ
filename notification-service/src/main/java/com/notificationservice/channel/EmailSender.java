package com.notificationservice.channel;

import com.notificationservice.enums.ChannelType;
import com.notificationservice.model.Notification;

/**
 * Delivers over email. In a real system this wraps a JavaMail / SES
 * client; here it prints to demonstrate the wiring. It reads the
 * recipient's emailAddress — the field this channel cares about — and
 * fails fast if the recipient has no email on file (you can't email
 * someone with no address; better to report failure than pretend success).
 */
public class EmailSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        String to = notification.getRecipient().getEmailAddress().orElse(null);
        if (to == null) {
            System.out.println("  [EMAIL] no email address for "
                    + notification.getRecipient().getUserId() + " — cannot send");
            return false;
        }
        System.out.println("  [EMAIL] to=" + to
                + " | subject=\"" + notification.getSubject() + "\""
                + " | body=\"" + notification.getBody() + "\"");
        return true;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }
}
