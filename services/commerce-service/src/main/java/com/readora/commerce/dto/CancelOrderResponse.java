package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

public record CancelOrderResponse(UUID orderId, String status, Instant cancelledAt) {
}
