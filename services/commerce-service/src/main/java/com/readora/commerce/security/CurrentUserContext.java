package com.readora.commerce.security;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user in request context"));
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
