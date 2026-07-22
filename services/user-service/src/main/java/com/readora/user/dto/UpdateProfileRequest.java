package com.readora.user.dto;

import java.util.List;
import java.util.UUID;

/** Every field is applied — send the current value for anything you don't intend to change. */
public record UpdateProfileRequest(
        String displayName,
        String phone,
        UUID preferredStoreId,
        List<UUID> favoriteCategoryIds
) {
}
