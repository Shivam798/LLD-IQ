package com.elevatorsystem.enums;

/**
 * INTERNAL — button pressed inside the cabin (user selects destination floor).
 * EXTERNAL — hall call from a floor (user requests an elevator).
 */
public enum RequestSource {
    INTERNAL,
    EXTERNAL
}
