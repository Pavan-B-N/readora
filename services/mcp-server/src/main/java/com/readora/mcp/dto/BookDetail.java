package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookDetail(
        String id, String title, String publisher, BigDecimal listPrice,
        String currency, Availability availability
) {
    public record Availability(String status, int quantityAvailable) {
    }
}
