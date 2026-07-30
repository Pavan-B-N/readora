package com.readora.commerce.dto;

import java.util.List;
import java.util.UUID;

// Request to catalog-service to look up virtual-edition availability for a set of books.
public record VirtualEditionLookupRequest(List<UUID> bookIds) {
}
