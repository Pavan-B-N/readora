package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Result of a successful checkout: the created order plus its pricing breakdown.
public record CheckoutResponse(
        UUID orderId,
        String orderNumber,
        String status,
        String deliveryType,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal packagingFee,
        BigDecimal taxAmount,
        BigDecimal grandTotal,
        BigDecimal walletAmountUsed,
        String paymentMethod,
        String currency,
        Instant placedAt
) {
}
