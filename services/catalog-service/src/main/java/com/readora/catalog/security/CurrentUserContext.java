package com.readora.catalog.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> CURRENT_ROLES = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UUID userId, List<String> roles) {
        CURRENT_USER.set(userId);
        CURRENT_ROLES.set(roles);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static boolean hasRole(String role) {
        List<String> roles = CURRENT_ROLES.get();
        return roles != null && roles.contains(role);
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLES.remove();
    }
}
