package com.readora.commerce.dto;

import java.util.UUID;

/** Mirrors one entry of catalog-service's BookCoverLookupResponse. */
public record BookCover(UUID id, String coverImageUrl) {
}
