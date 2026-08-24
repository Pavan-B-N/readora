package com.readora.auth.entity;

/** The set of role codes a {@link Role} can hold. */
public enum RoleCode {

    CUSTOMER("Default role for registered customers"),
    ADMIN("Administrative role with elevated privileges");

    private final String description;

    RoleCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
