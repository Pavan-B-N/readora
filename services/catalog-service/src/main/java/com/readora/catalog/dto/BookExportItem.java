package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

public record BookExportItem(UUID id, String title, List<String> authors, String description, String tableOfContents) {
}
