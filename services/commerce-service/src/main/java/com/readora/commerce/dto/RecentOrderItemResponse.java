package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

// A recently-ordered book and its current order status.
public record RecentOrderItemResponse(UUID bookId, String status, Instant placedAt) {
}
