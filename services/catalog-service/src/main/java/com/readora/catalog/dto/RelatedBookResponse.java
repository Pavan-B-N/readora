package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RelatedBookResponse(UUID id, String title, BigDecimal listPrice, String coverImageUrl) {
}
