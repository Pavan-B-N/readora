package com.readora.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

// Per-route rate limit rules, config-driven via app.rate-limit.* in each service's application.yml.
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        RateLimitRule defaultRule,
        Map<String, RateLimitRule> rules
) {

    // Looks up the configured rule for a route id, falling back to the default rule.
    public RateLimitRule ruleFor(String routeId) {
        if (rules != null && rules.containsKey(routeId)) {
            return rules.get(routeId);
        }
        return defaultRule;
    }

    // A request limit allowed within a rolling time window.
    public record RateLimitRule(int limit, int windowSeconds) {
    }
}
