package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// A page of a user's order history.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPage(List<OrderSummary> items) {
}
