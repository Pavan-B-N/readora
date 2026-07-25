package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

// Request to mark a batch of books as embedded.
public record MarkEmbeddedRequest(List<UUID> bookIds) {
}
