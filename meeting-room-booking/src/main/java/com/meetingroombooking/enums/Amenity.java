package com.meetingroombooking.enums;

/**
 * Equipment a meeting room may provide. Modelled as an enum (not free-form
 * strings) so a booking request can only ask for amenities that actually
 * exist, and room matching is a simple set containment check.
 */
public enum Amenity {
    PROJECTOR,
    WHITEBOARD,
    VIDEO_CONFERENCE,
    CONFERENCE_PHONE
}
