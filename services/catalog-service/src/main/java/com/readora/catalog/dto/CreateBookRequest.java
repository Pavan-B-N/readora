package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateBookRequest(
        @NotBlank @Size(min = 13, max = 13) String isbn13,
        @NotBlank String title,

        @NotBlank String description,
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
        @URL String coverImageUrl
) {
}
