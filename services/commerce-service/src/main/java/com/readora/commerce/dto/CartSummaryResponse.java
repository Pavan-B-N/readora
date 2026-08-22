package com.readora.commerce.dto;

import java.math.BigDecimal;

public record CartSummaryResponse(int itemCount, BigDecimal subtotal, String currency) {
}
