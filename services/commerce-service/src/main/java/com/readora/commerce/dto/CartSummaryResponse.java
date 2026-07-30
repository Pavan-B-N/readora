package com.readora.commerce.dto;

import java.math.BigDecimal;

// Lightweight cart totals, e.g. for a header badge.
public record CartSummaryResponse(int itemCount, BigDecimal subtotal, String currency) {
}
