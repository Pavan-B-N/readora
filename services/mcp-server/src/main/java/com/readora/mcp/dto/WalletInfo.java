package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

// A user's wallet balance.
@JsonIgnoreProperties(ignoreUnknown = true)
public record WalletInfo(BigDecimal balance, String currency) {
}
