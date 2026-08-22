package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderSummary(String orderId, String orderNumber, String status, BigDecimal grandTotal, Instant placedAt) {
}
