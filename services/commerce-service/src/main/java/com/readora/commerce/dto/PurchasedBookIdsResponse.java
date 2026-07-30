package com.readora.commerce.dto;

import java.util.List;
import java.util.UUID;

// The set of book ids the caller has purchased.
public record PurchasedBookIdsResponse(List<UUID> bookIds) {
}
