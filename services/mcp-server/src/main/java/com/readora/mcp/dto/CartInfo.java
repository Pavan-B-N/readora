package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartInfo(List<Item> items, BigDecimal subtotal, String currency, int itemCount) {
    public record Item(String bookId, String title, int qty, BigDecimal unitPrice) {
    }
}
