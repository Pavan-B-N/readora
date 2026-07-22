package com.readora.user.dto;

import java.util.UUID;

/** {@code userId} is null when the store has no assigned admin. */
public record StoreAdminResponse(UUID userId) {
}
