package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

public record PurchasedBookIdsResponse(List<UUID> bookIds) {
}
