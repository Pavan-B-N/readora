package com.readora.catalog.dto;

import java.util.List;

public record BookExportPage(List<BookExportItem> items, int totalPages) {
}
