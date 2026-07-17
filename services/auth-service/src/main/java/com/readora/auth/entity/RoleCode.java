package com.readora.auth.entity;

/** The set of role codes a {@link Role} can hold. */
public enum RoleCode {

    CUSTOMER("Default role for registered customers"),
    ADMIN("Administrative role with elevated privileges"),
    DELIVERY_AGENT("Delivers physical orders assigned to them");

    private final String description;

    // Attaches the description to this constant.
    RoleCode(String description) {
        this.description = description;
    }

    // Human-readable description of the role code.
    public String getDescription() {
        return description;
    }
}
