package com.readora.sharedcore.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Holds the caller's identity for the life of one request via ThreadLocals — populated by JwtAuthenticationFilter, cleared in its finally block.
public final class CurrentUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> CURRENT_ROLES = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_EMAIL = new ThreadLocal<>();

    // Static-only utility class.
    private CurrentUserContext() {
    }

    // Sets the full identity for the current thread.
    public static void set(UUID userId, List<String> roles, String email) {
        CURRENT_USER.set(userId);
        CURRENT_ROLES.set(roles);
        CURRENT_EMAIL.set(email);
    }

    // Overload for callers with no email to set.
    public static void set(UUID userId, List<String> roles) {
        set(userId, roles, null);
    }

    // Overload for callers with no roles or email to set.
    public static void set(UUID userId) {
        set(userId, List.of(), null);
    }

    // The current user, if a JWT was validated for this request.
    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    // Like get(), but throws if called somewhere that's supposed to guarantee an authenticated caller.
    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user in request context"));
    }

    // Whether the current caller carries the given role.
    public static boolean hasRole(String role) {
        List<String> roles = CURRENT_ROLES.get();
        return roles != null && roles.contains(role);
    }

    // The current caller's email, if the JWT carried one.
    public static Optional<String> getEmail() {
        return Optional.ofNullable(CURRENT_EMAIL.get());
    }

    // Removes all ThreadLocals — must be called at the end of every request to avoid leaking identity into the next request on a reused thread.
    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLES.remove();
        CURRENT_EMAIL.remove();
    }
}
