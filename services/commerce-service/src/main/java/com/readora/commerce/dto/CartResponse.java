package com.readora.commerce.dto;

import com.readora.commerce.entity.DeliveryType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// The caller's current cart contents and running totals.
public record CartResponse(
        List<Item> items, BigDecimal subtotal, String currency, int itemCount, boolean requiresShippingAddress
) {
    // One priced line item in the cart.
    public record Item(UUID bookId, String title, int qty, BigDecimal unitPrice, BigDecimal lineTotal, DeliveryType deliveryType) {
    }
}
