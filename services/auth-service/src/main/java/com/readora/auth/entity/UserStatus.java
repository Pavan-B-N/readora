package com.readora.auth.entity;

/** LOCKED is set automatically by AuthService after too many failed logins; nothing currently clears it. */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    DISABLED
}
