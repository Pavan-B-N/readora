package com.readora.catalog.dto;

import java.util.UUID;

public record BookUpsertedEvent(UUID bookId) {
}
