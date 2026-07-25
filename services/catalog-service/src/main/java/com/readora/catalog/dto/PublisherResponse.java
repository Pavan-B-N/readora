package com.readora.catalog.dto;

import java.util.UUID;

// API response for a publisher.
public record PublisherResponse(UUID id, String name, String slug) {
}
