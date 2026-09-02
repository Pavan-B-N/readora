package com.readora.sharedcore.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Holds the caller's identity for the life of one request — populated by JwtAuthenticationFilter
 * after validating the caller's JWT, cleared in its finally block. Always carries roles and email
 * (empty/null when a particular JWT doesn't have them, or a service never checks them) rather
 * than each service defining its own narrower variant — costs nothing for a service that only
 * ever calls require(), and means JwtAuthenticationFilter doesn't need a service-specific variant.
 */
public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> CURRENT_ROLES = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_EMAIL = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UUID userId, List<String> roles, String email) {
        CURRENT_USER.set(userId);
        CURRENT_ROLES.set(roles);
        CURRENT_EMAIL.set(email);
    }

    public static void set(UUID userId, List<String> roles) {
        set(userId, roles, null);
    }

    public static void set(UUID userId) {
        set(userId, List.of(), null);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user in request context"));
    }

    public static boolean hasRole(String role) {
        List<String> roles = CURRENT_ROLES.get();
        return roles != null && roles.contains(role);
    }

    public static Optional<String> getEmail() {
        return Optional.ofNullable(CURRENT_EMAIL.get());
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLES.remove();
        CURRENT_EMAIL.remove();
    }
}
