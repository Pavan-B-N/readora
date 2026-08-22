package com.readora.payment.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the caller's user id for the life of one request. Populated by UserContextFilter from
 * X-User-Id (set by api-gateway after validating the caller's JWT). This service trusts that
 * header rather than re-validating the JWT itself — see the batch build summary for why.
 */
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
