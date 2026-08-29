package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Internal, delivery-agent-service-only view of an order — unlike the customer-facing
 * OrderDetailResponse.ShippingAddress, this carries the FULL address snapshot (line2, state,
 * phone) an agent actually needs to make a delivery. grandTotal lets delivery-agent-service
 * compute a payout that scales with order value instead of a flat rate.
 */
public record OrderDeliveryDetailResponse(
        UUID orderId,
        String orderNumber,
        String status,
        UUID storeId,
        ShippingAddress shippingAddress,
        List<Item> items,
        BigDecimal grandTotal,
        Instant placedAt
) {
    public record ShippingAddress(
            String recipientName, String line1, String line2, String city, String state,
            String postalCode, String countryCode, String phone
    ) {
    }

    public record Item(String title, int qty) {
    }
}
