package com.readora.user.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        String displayName,
        String avatarUrl,
        String locale,
        WalletSummary wallet
) {
    public record WalletSummary(BigDecimal balance, String currency) {
    }
}
