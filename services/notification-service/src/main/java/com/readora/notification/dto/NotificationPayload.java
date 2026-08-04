package com.readora.notification.dto;

import java.time.Instant;

// A live WebSocket push envelope: notification type, its data payload, and when it was sent.
public record NotificationPayload(String type, Object data, Instant timestamp) {
    // Convenience constructor that stamps the current time.
    public NotificationPayload(String type, Object data) {
        this(type, data, Instant.now());
    }
}
