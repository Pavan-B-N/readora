package com.readora.notification.dto;

import java.time.Instant;
import java.util.UUID;

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
