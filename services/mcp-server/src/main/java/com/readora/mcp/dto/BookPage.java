package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// A page of book search results.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookPage(List<BookSummary> items) {
}
