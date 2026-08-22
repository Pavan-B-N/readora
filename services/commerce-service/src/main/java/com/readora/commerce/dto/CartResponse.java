package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(List<Item> items, BigDecimal subtotal, String currency, int itemCount) {
    public record Item(UUID bookId, String title, int qty, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
