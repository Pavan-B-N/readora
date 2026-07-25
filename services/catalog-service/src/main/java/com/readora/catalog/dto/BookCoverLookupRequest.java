package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

// Request to look up cover images for a batch of book ids.
public record BookCoverLookupRequest(List<UUID> bookIds) {
}
