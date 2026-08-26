package com.readora.user.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(BigDecimal balance, String currency) {
}
