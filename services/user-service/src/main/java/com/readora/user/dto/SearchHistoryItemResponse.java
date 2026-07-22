package com.readora.user.dto;

import java.time.Instant;

// A single search history entry.
public record SearchHistoryItemResponse(String query, Instant searchedAt) {
}
