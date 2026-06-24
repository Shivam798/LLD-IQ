package com.notificationservice.factory;

import com.notificationservice.channel.EmailSender;
import com.notificationservice.channel.NotificationSender;
import com.notificationservice.channel.PushSender;
import com.notificationservice.channel.SmsSender;
import com.notificationservice.enums.ChannelType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that maps a ChannelType to the sender that handles it.
 * <p>
 * Why a factory at all: the service knows it wants to send over EMAIL,
 * but it should NOT know that EMAIL means `new EmailSender()`. Centralizing
 * that decision here means:
 *   - the service depends only on the ChannelType enum and the
 *     NotificationSender interface (Dependency Inversion);
 *   - adding a channel is one register() call, not an edit to a switch
 *     scattered through the service (Open/Closed).
 * <p>
 * Why EnumMap: keys are enum constants, so EnumMap is the tightest,
 * fastest map possible — backed by a plain array indexed by ordinal,
 * no hashing.
 * <p>
 * Senders are STATELESS (they hold no per-notification data), so we keep
 * one shared instance per channel rather than building a new sender per
 * send — cheaper and perfectly thread-safe.
 */
public class NotificationSenderFactory {

    private final Map<ChannelType, NotificationSender> senders = new EnumMap<>(ChannelType.class);

    public NotificationSenderFactory() {
        // Pre-register the built-in channels. A new channel slots in via
        // register(...) below without touching this constructor.
        register(new EmailSender());
        register(new SmsSender());
        register(new PushSender());
    }

    /**
     * Register (or override) the sender for a channel. Open/Closed in
     * action: WhatsApp support = write WhatsAppSender, call register(it).
     */
    public void register(NotificationSender sender) {
        senders.put(sender.getChannelType(), sender);
    }

    public NotificationSender getSender(ChannelType channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender registered for channel: " + channel);
        }
        return sender;
    }
}
