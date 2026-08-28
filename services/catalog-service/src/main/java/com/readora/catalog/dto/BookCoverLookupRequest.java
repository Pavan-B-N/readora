package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

public record BookCoverLookupRequest(List<UUID> bookIds) {
}
