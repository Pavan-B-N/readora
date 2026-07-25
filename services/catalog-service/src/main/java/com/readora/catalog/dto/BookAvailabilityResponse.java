package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

/** Subset of the requested bookIds that are actually purchasable — in stock at the given store, or virtual. */
public record BookAvailabilityResponse(List<UUID> availableBookIds) {
}
