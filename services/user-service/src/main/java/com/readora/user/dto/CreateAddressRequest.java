package com.readora.user.dto;

import com.readora.user.entity.AddressLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAddressRequest(
        @NotNull AddressLabel label,
        @NotBlank String recipientName,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank String countryCode,
        String phone,
        boolean isDefault
) {
}
