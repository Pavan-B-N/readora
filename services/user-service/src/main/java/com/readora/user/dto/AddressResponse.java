package com.readora.user.dto;

import com.readora.user.entity.AddressLabel;
import com.readora.user.entity.AddressRecipientType;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        AddressLabel label,
        AddressRecipientType recipientType,
        String recipientName,
        String recipientPhone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String countryCode,
        UUID storeId,
        boolean isDefault
) {
}
