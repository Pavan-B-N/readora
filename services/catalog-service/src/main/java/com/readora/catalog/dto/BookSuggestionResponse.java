package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Lighter than BookSummaryResponse — just enough for a typeahead dropdown row. */
public record BookSuggestionResponse(
        UUID id, String title, List<String> authors, BigDecimal listPrice, String currency, String coverImageUrl
) {
}
