package com.readora.catalog.dto;

import java.util.UUID;

public record StoreResponse(
        UUID id,
        String name,
        String city,
        String line1,
        String line2,
        String state,
        String postalCode,
        String countryCode
) {
}
