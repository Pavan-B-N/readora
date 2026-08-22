package com.readora.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        RateLimitRule defaultRule,
        Map<String, RateLimitRule> rules
) {

    public RateLimitRule ruleFor(String routeId) {
        if (rules != null && rules.containsKey(routeId)) {
            return rules.get(routeId);
        }
        return defaultRule;
    }

    public record RateLimitRule(int limit, int windowSeconds) {
    }
}
