package com.splitwise.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named collection of users (e.g., "Friends Trip", "Roommates"). Used as the
 * scoping unit for group-level operations like debt simplification — we only
 * net out balances *within* the group's membership, not across a user's whole life.
 *
 * The group itself doesn't own expenses or balances; those live on User and are
 * computed/filtered by group membership when needed. This keeps Group light and
 * avoids the headache of keeping two sources of truth in sync.
 */
public class Group {
    private final String id;
    private final String name;
    private final List<User> members;

    public Group(String name, List<User> members) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        // Defensive copy at construction — caller cannot later mutate the list
        // and quietly change group membership.
        this.members = new ArrayList<>(members);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a defensive copy so callers iterating over members can't
     * structurally modify the underlying list (and we don't have to expose
     * an unmodifiable view + worry about its contract drift).
     */
    public List<User> getMembers() {
        return new ArrayList<>(members);
    }
}
