package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentOrderItemResponse(UUID bookId, String status, Instant placedAt) {
}
