package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBookRequest(
        @NotBlank String isbn13,
        @NotBlank String title,
        String subtitle,
        String description,
        String tableOfContents,
        UUID categoryId,
        UUID publisherId,
        UUID storeId,
        @NotEmpty List<UUID> authorIds,
        String language,
        Integer pageCount,
        LocalDate publishedOn,
        @NotNull BigDecimal listPrice,
        @NotBlank String currency,
        String coverImageUrl
) {
}
