package com.readora.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Wallet balance plus a page of the transaction ledger.
public record WalletResponse(
        BigDecimal balance,
        String currency,
        List<Item> items
) {
    // A single wallet ledger entry.
    public record Item(UUID id, BigDecimal amount, String type, BigDecimal balanceAfter, UUID orderId, Instant createdAt) {
    }
}
