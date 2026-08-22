package com.readora.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WalletResponse(
        BigDecimal balance,
        String currency,
        List<Item> items
) {
    public record Item(UUID id, BigDecimal amount, String type, BigDecimal balanceAfter, UUID orderId, Instant createdAt) {
    }
}
