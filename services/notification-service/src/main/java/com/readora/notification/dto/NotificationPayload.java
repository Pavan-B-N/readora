package com.readora.notification.dto;

import java.time.Instant;

public record NotificationPayload(String type, Object data, Instant timestamp) {
    public NotificationPayload(String type, Object data) {
        this(type, data, Instant.now());
    }
}
