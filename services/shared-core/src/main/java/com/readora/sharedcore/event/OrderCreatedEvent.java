package com.readora.sharedcore.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Published by commerce-service at checkout; consumed by payment-service to charge the order. */
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
