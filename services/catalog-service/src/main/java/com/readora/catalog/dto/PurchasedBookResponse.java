package com.readora.catalog.dto;

import java.time.Instant;

/** One "Your orders" rail entry — a book plus the status of the order it came from. */
public record PurchasedBookResponse(BookSummaryResponse book, String orderStatus, Instant placedAt) {
}
