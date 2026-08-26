package com.readora.commerce.cart;

import com.readora.commerce.entity.DeliveryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemData(
        UUID bookId, String title, int qty, BigDecimal unitPriceSnapshot, DeliveryType deliveryType, Instant addedAt
) {

    public CartItemData withQty(int newQty) {
        return new CartItemData(bookId, title, newQty, unitPriceSnapshot, deliveryType, addedAt);
    }
}
