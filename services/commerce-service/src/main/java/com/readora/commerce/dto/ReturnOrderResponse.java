package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

// Result of returning an order: its new status and when the return was recorded.
public record ReturnOrderResponse(UUID orderId, String status, Instant returnedAt) {
}
