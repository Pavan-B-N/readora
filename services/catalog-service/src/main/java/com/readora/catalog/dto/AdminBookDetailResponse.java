package com.readora.catalog.dto;

import com.readora.catalog.entity.VirtualFileFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Everything the admin edit form needs to prefill — unlike BookDetailResponse, includes fields with no public read use (tableOfContents, coverImageUrl) plus current inventory/virtual-edition state. */
public record AdminBookDetailResponse(
        UUID id,
        String isbn13,
        String title,

        String description,
        String tableOfContents,
        UUID categoryId,
        UUID publisherId,
        UUID storeId,
        List<UUID> authorIds,
        String language,
        Integer pageCount,
        LocalDate publishedOn,
        BigDecimal listPrice,
        String currency,
        String coverImageUrl,
        boolean isActive,
        UUID createdByUserId,
        Instant createdAt,
        Instant embeddedAt,
        boolean needsReembedding,
        Inventory inventory,
        VirtualEdition virtualEdition
) {
    public record Inventory(int qtyOnHand, int qtyReserved, int reorderThreshold) {
    }

    public record VirtualEdition(
            String fileUrl,
            VirtualFileFormat fileFormat,
            Long fileSizeBytes,
            BigDecimal price,
            String currency,
            boolean isActive,
            UUID createdByUserId
    ) {
    }
}
