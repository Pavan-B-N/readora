package com.readora.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(List<String> allowedOrigins) {
}
