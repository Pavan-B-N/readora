package com.readora.catalog.dto;

import java.util.List;

// One page of the book text export.
public record BookExportPage(List<BookExportItem> items, int totalPages) {
}
