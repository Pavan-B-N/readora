package com.readora.payment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        List<Item> items,
        BigDecimal grandTotal,
        BigDecimal walletAmountToUse,
        String paymentMethod
) {
    public record Item(UUID bookId, int qty) {
    }
}
