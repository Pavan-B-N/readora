package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// One row of the caller's order history list.
public record OrderSummaryResponse(
        UUID orderId,
        String orderNumber,
        String status,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt,
        boolean cancellable,
        Instant deliveredAt,
        /** Capped preview of this order's line items, for a book-cover collage — see itemCount for the true total. */
        List<ItemPreview> itemPreviews,
        int itemCount
) {
    // One item shown in the cover-collage preview.
    public record ItemPreview(UUID bookId, String title, String coverImageUrl) {
    }
}
