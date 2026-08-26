package com.readora.user.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        String phone,
        String locale,
        UUID preferredStoreId,
        UUID adminStoreId,
        List<UUID> favoriteCategoryIds,
        WalletSummary wallet
) {
    public record WalletSummary(BigDecimal balance, String currency) {
    }
}
