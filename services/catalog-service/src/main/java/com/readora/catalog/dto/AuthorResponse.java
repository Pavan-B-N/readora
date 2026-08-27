package com.readora.catalog.dto;

import java.util.UUID;

public record AuthorResponse(UUID id, String name, String slug, String bio, String photoUrl) {
}
