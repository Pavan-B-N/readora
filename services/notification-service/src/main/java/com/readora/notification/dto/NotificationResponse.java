package com.readora.notification.dto;

import java.time.Instant;
import java.util.UUID;

// A single notification as returned to API callers.
public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        UUID orderId,
        boolean read,
        Instant createdAt
) {
}
