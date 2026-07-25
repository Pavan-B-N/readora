package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

// Request to look up book details for a batch of book ids.
public record BookLookupRequest(List<UUID> bookIds) {
}
