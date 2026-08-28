package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

/** storeId is nullable — callers with no store context (e.g. a guest who hasn't picked one) get virtual-only availability. */
public record BookAvailabilityRequest(List<UUID> bookIds, UUID storeId) {
}
