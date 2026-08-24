package com.readora.catalog.dto;

import java.util.List;

public record BookLookupResponse(List<BookExportItem> items) {
}
