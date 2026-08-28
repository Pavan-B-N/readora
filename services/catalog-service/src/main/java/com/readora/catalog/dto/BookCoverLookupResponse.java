package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

public record BookCoverLookupResponse(List<Item> items) {
    public record Item(UUID id, String coverImageUrl) {
    }
}
