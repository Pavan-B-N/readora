package com.readora.commerce.dto;

import com.readora.commerce.entity.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Deviates from the doc's request shape: takes the full shipping address inline instead of an
 * addressId that would need a lookup against user-service. See the build summary for why.
 * shippingAddress is required only when deliveryType is PHYSICAL — validated in OrderService,
 * not here, since it's a cross-field rule bean validation doesn't express well.
 */
public record CheckoutRequest(
        @NotNull DeliveryType deliveryType,
        @Valid ShippingAddress shippingAddress,
        @NotBlank String paymentMethod,
        @NotEmpty List<Item> items
) {
    public record ShippingAddress(
            @NotBlank String recipientName,
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String postalCode,
            @NotBlank String countryCode,
            String phone
    ) {
    }

    public record Item(@NotNull UUID bookId, int qty) {
    }
}
