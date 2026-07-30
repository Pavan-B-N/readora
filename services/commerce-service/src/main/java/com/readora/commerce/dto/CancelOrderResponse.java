package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

// Response confirming an order was cancelled.
public record CancelOrderResponse(UUID orderId, String status, Instant cancelledAt) {
}
