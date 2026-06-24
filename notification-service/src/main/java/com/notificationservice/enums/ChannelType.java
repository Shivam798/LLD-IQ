package com.notificationservice.enums;

/**
 * The transport a notification travels over.
 *
 * Why an enum and not a String: the set of channels is small, fixed, and
 * known at compile time. An enum gives us exhaustive switch coverage, a
 * safe map key for the sender factory, and zero chance of a typo like
 * "emai" silently routing nowhere. Adding a new channel (e.g. WHATSAPP,
 * SLACK) is one new constant plus one new sender — no existing code changes.
 */
public enum ChannelType {
    EMAIL,
    SMS,
    PUSH
}
