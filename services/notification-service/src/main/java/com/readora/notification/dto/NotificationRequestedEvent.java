package com.readora.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Mirrors commerce-service's event of the same name — a generic, arbitrarily-targeted notification. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationRequestedEvent(UUID userId, String type, String title, String message, UUID orderId) {
}
