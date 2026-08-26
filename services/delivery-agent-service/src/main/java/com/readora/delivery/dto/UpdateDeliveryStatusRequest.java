package com.readora.delivery.dto;

import java.util.UUID;

/** Mirrors commerce-service's request of the same name — sent to its internal PUT endpoint. */
public record UpdateDeliveryStatusRequest(String status, UUID deliveryAgentId, String deliveryAgentName) {
}
