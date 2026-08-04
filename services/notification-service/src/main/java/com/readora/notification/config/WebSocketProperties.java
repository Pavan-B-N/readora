package com.readora.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Origins allowed to open a WebSocket connection, config-driven via app.websocket.* in this service's application.yml.
@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(List<String> allowedOrigins) {
}
