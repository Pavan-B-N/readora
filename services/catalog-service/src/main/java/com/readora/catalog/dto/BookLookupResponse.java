package com.readora.catalog.dto;

import java.util.List;

// Response carrying the looked-up books' text content.
public record BookLookupResponse(List<BookExportItem> items) {
}
