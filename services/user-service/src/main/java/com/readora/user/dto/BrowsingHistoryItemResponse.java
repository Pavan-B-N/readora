package com.readora.user.dto;

import java.time.Instant;
import java.util.UUID;

public record BrowsingHistoryItemResponse(UUID bookId, Instant viewedAt) {
}
