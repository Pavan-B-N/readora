package com.readora.user.security;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_EMAIL = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UUID userId, String email) {
        CURRENT_USER.set(userId);
        CURRENT_EMAIL.set(email);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user in request context"));
    }

    public static Optional<String> getEmail() {
        return Optional.ofNullable(CURRENT_EMAIL.get());
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_EMAIL.remove();
    }
}
