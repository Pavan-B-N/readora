package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookSummary(
        String id, String title, List<String> authors, BigDecimal listPrice, String currency, String availability
) {
}
