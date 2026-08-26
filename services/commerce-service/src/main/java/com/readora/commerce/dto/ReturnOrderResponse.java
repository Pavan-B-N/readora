package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

public record ReturnOrderResponse(UUID orderId, String status, Instant returnedAt) {
}
