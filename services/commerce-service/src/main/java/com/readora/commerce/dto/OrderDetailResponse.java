package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Full detail for a single order: line items, address, status history, and payment/return info.
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
        Instant placedAt,
        String deliveryAgentName,
        Instant deliveredAt,
        /** null when payment-service hasn't recorded a payment for this order yet, or was unreachable. */
        PaymentInfo payment,
        /** Set once a return reaches RETURN_ASSIGNED or later — the agent collecting the return, distinct from deliveryAgentName above. */
        String returnAgentName
) {
    /** transactionId is payment-service's own payment id — there's no real external gateway behind this dummy provider. */
    public record PaymentInfo(
            UUID transactionId, String status, BigDecimal amount, BigDecimal walletAmountUsed,
            Instant authorizedAt, Instant capturedAt
    ) {
    }

    // One priced line item on the order.
    public record Item(
            UUID bookId, String title, String isbn13, int qty, BigDecimal unitPrice, BigDecimal lineTotal,
            String deliveryType
    ) {
    }

    // Customer-facing address snapshot (omits fields only a delivery agent needs).
    public record ShippingAddress(String recipientName, String line1, String city, String postalCode, String countryCode) {
    }

    // One status transition and when it happened.
    public record HistoryEntry(String toStatus, Instant at) {
    }
}
