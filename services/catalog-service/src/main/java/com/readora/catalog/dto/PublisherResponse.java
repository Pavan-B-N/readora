package com.readora.catalog.dto;

import java.util.UUID;

public record PublisherResponse(UUID id, String name, String slug) {
}
