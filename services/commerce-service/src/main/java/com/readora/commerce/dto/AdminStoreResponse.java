package com.readora.commerce.dto;

import java.util.UUID;

/** Mirrors user-service's internal response — storeId is null if the admin is unassigned. */
public record AdminStoreResponse(UUID storeId) {
}
