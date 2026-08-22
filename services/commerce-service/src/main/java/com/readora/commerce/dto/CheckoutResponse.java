package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        String orderNumber,
        String status,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt
) {
}
