package com.readora.user.dto;

import com.readora.user.entity.AddressLabel;
import com.readora.user.entity.AddressRecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAddressRequest(
        @NotNull AddressLabel label,
        @NotNull AddressRecipientType recipientType,
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank String countryCode,
        UUID storeId,
        boolean isDefault
) {
}
