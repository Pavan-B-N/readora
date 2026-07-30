package com.readora.commerce.cart;

import com.readora.commerce.entity.DeliveryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// One line item in a user's Redis-backed cart.
public record CartItemData(
        UUID bookId, String title, int qty, BigDecimal unitPriceSnapshot, DeliveryType deliveryType, Instant addedAt
) {

    // Returns a copy of this item with the quantity replaced.
    public CartItemData withQty(int newQty) {
        return new CartItemData(bookId, title, newQty, unitPriceSnapshot, deliveryType, addedAt);
    }
}
