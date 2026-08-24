package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

public record BookLookupRequest(List<UUID> bookIds) {
}
