package com.readora.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Ant-style route patterns that skip authentication entirely, config-driven via app.security.* in each service's application.yml.
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> publicRoutes) {
}
