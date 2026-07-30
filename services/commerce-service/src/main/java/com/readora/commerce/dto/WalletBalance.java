package com.readora.commerce.dto;

import java.math.BigDecimal;

// Mirrors user-service's wallet balance response.
public record WalletBalance(BigDecimal balance, String currency) {
}
