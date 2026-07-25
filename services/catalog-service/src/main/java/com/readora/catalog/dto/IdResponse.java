package com.readora.catalog.dto;

import java.util.UUID;

// Generic response wrapping a created/updated entity's id.
public record IdResponse(UUID id) {
}
