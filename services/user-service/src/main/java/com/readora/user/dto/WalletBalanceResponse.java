package com.readora.user.dto;

import java.math.BigDecimal;

// A wallet balance snapshot.
public record WalletBalanceResponse(BigDecimal balance, String currency) {
}
