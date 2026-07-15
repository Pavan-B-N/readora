package com.readora.sharedcore.event;

import java.util.UUID;

/** Published by catalog-service on book create/update; consumed by ai-service to refresh embeddings. */
public record BookUpsertedEvent(UUID bookId) {
}
