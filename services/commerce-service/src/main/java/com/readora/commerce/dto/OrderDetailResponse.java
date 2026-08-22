package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderNumber,
        String status,
        List<Item> items,
        ShippingAddress shippingAddress,
        List<HistoryEntry> history,
        boolean cancellable,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt
) {
    public record Item(UUID bookId, String title, String isbn13, int qty, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record ShippingAddress(String recipientName, String line1, String city, String postalCode, String countryCode) {
    }

    public record HistoryEntry(String toStatus, Instant at) {
    }
}
