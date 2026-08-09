package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

// A user's current shopping cart.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartInfo(List<Item> items, BigDecimal subtotal, String currency, int itemCount) {
    // A single line item in the cart.
    public record Item(String bookId, String title, int qty, BigDecimal unitPrice) {
    }
}
