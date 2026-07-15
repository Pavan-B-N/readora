package com.readora.sharedcore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Backs UserContextFilter, config-driven per service via app.security.* in each service's application.yml.
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        List<String> publicRoutes, // Ant-style patterns that skip authentication entirely, for every HTTP method
        List<String> publicGetRoutes, // Ant-style patterns that are public for GET only, e.g. publicly-readable reviews
        List<RoleGate> roleGates // (pathPrefix, role) pairs requiring an authenticated caller with that role
) {
    // Defaults null lists to empty so callers never need a null check.
    public SecurityProperties {
        if (publicGetRoutes == null) {
            publicGetRoutes = List.of();
        }
        if (roleGates == null) {
            roleGates = List.of();
        }
    }

    // A path prefix that requires the caller to hold the given role.
    public record RoleGate(String pathPrefix, String role) {
    }
}
