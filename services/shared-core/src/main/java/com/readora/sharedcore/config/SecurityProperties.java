package com.readora.sharedcore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Backs UserContextFilter, config-driven per service via app.security.* in each service's
 * application.yml — no service-specific subclassing needed, even though the actual gated paths
 * and roles differ quite a bit from one service to the next (some have none at all).
 *
 * @param publicRoutes    Ant-style patterns that skip authentication entirely (matched against
 *                        every HTTP method)
 * @param publicGetRoutes Ant-style patterns that are public for GET only — e.g. a reviews list
 *                        that's publicly readable but not publicly postable. Empty for services
 *                        that don't need the distinction.
 * @param roleGates       (pathPrefix, role) pairs — a request whose path starts with pathPrefix
 *                        must carry an authenticated caller with that role. Matched by plain
 *                        String.startsWith, same as every service's original hand-written check.
 *                        Empty for services with no route beyond the plain auth requirement.
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        List<String> publicRoutes,
        List<String> publicGetRoutes,
        List<RoleGate> roleGates
) {
    public SecurityProperties {
        if (publicGetRoutes == null) {
            publicGetRoutes = List.of();
        }
        if (roleGates == null) {
            roleGates = List.of();
        }
    }

    public record RoleGate(String pathPrefix, String role) {
    }
}
