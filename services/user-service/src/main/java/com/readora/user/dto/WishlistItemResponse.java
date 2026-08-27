package com.readora.user.dto;

import java.time.Instant;
import java.util.UUID;

public record WishlistItemResponse(UUID bookId, Instant addedAt) {
}
