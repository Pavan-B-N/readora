package com.readora.commerce.dto;

import java.util.List;
import java.util.UUID;

public record VirtualEditionLookupRequest(List<UUID> bookIds) {
}
