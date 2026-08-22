package com.readora.commerce.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemData(UUID bookId, String title, int qty, BigDecimal unitPriceSnapshot, Instant addedAt) {

    public CartItemData withQty(int newQty) {
        return new CartItemData(bookId, title, newQty, unitPriceSnapshot, addedAt);
    }
}
