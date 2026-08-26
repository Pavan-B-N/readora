package com.readora.delivery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Mirrors commerce-service's internal response of the same name. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderDeliveryDetailResponse(
        UUID orderId,
        String orderNumber,
        String status,
        UUID storeId,
        ShippingAddress shippingAddress,
        List<Item> items,
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
