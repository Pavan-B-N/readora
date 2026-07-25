package com.readora.catalog.dto;

import java.time.Instant;
import java.util.UUID;

/** Mirrors commerce-service's internal /orders/recent-items response — one order line item, newest first. */
public record RecentOrderItemResponse(UUID bookId, String status, Instant placedAt) {
}
