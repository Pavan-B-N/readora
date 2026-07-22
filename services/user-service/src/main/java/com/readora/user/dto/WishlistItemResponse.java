package com.readora.user.dto;

import java.time.Instant;
import java.util.UUID;

// A single wishlist entry.
public record WishlistItemResponse(UUID bookId, Instant addedAt) {
}
