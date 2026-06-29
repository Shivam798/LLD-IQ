package com.meetingroombooking.model;

/**
 * A person who organizes or attends meetings. Immutable value object — a user
 * is identified by {@code id}; name/email are contact details observers use.
 */
public class User {

    private final String id;
    private final String name;
    private final String email;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return name;
    }
}
