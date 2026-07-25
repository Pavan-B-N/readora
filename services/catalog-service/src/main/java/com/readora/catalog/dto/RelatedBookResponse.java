package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

// A related book, as shown on a book's detail page.
public record RelatedBookResponse(UUID id, String title, BigDecimal listPrice, String coverImageUrl) {
}
