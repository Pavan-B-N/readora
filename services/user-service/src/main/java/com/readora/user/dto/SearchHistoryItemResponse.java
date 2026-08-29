package com.readora.user.dto;

import java.time.Instant;

public record SearchHistoryItemResponse(String query, Instant searchedAt) {
}
