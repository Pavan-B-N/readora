package com.readora.user.dto;

import java.util.UUID;

/** {@code storeId} is null when the user isn't an admin, or is an admin not yet assigned a store. */
public record AdminStoreResponse(UUID storeId) {
}
