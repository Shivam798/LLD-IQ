package com.notificationservice.channel;

import com.notificationservice.enums.ChannelType;
import com.notificationservice.model.Notification;

/**
 * Delivers a mobile/web push notification. Reads the recipient's
 * deviceToken (the APNs/FCM registration id). If the device token is
 * missing the user simply isn't reachable on this channel, so we report
 * failure and let the caller decide whether to fall back to another channel.
 */
public class PushSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        String token = notification.getRecipient().getDeviceToken().orElse(null);
        if (token == null) {
            System.out.println("  [PUSH] no device token for "
                    + notification.getRecipient().getUserId() + " — cannot send");
            return false;
        }
        System.out.println("  [PUSH] device=" + token
                + " | title=\"" + notification.getSubject() + "\""
                + " | body=\"" + notification.getBody() + "\"");
        return true;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.PUSH;
    }
}
