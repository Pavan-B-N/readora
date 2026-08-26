package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Every field is applied — send the current value for anything you don't intend to change. */
public record UpdateBookRequest(
        @NotBlank String title,
        String subtitle,
        String description,
        String tableOfContents,
        UUID categoryId,
        UUID publisherId,
        List<UUID> authorIds,
        String language,
        Integer pageCount,
        LocalDate publishedOn,
        @NotNull BigDecimal listPrice,
        @NotBlank String currency,
        String coverImageUrl,
        boolean isActive
) {
}
