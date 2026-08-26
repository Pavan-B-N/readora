package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderNumber,
        String status,
        String deliveryType,
        List<Item> items,
        ShippingAddress shippingAddress,
        List<HistoryEntry> history,
        boolean cancellable,
        boolean returnable,
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
    public record Item(
            UUID bookId, String title, String isbn13, int qty, BigDecimal unitPrice, BigDecimal lineTotal,
            String deliveryType
    ) {
    }

    public record ShippingAddress(String recipientName, String line1, String city, String postalCode, String countryCode) {
    }

    public record HistoryEntry(String toStatus, Instant at) {
    }
}
