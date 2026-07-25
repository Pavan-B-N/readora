package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

// Mirrors commerce-service's internal response of purchased book ids for a user.
public record PurchasedBookIdsResponse(List<UUID> bookIds) {
}
