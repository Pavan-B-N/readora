package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

// Response carrying cover images for the requested book ids.
public record BookCoverLookupResponse(List<Item> items) {
    // One book's cover image.
    public record Item(UUID id, String coverImageUrl) {
    }
}
