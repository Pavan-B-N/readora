package com.readora.commerce.dto;

import java.util.List;
import java.util.UUID;

// Request to look up cover image URLs for a batch of book ids.
public record BookCoverLookupRequest(List<UUID> bookIds) {
}
