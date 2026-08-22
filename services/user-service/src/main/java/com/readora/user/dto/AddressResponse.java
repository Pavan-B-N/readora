package com.readora.user.dto;

import com.readora.user.entity.AddressLabel;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        AddressLabel label,
        String recipientName,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String countryCode,
        boolean isDefault
) {
}
