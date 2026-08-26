package com.readora.catalog.dto;

import java.util.UUID;

/** Mirrors user-service's internal response — {@code storeId} is null if the admin is unassigned. */
public record AdminStoreResponse(UUID storeId) {
}
