package com.readora.commerce.dto;

import java.math.BigDecimal;

public record WalletBalance(BigDecimal balance, String currency) {
}
