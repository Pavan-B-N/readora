package com.readora.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** ignoreUnknown: commerce-service's event carries extra fields (deliveryType, storeId) this service doesn't need. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatusChangedEvent(UUID orderId, UUID userId, String orderNumber, String toStatus) {
}
