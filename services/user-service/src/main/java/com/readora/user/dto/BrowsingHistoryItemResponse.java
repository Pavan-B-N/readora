package com.readora.user.dto;

import java.time.Instant;
import java.util.UUID;

// A single browsing history entry.
public record BrowsingHistoryItemResponse(UUID bookId, Instant viewedAt) {
}
