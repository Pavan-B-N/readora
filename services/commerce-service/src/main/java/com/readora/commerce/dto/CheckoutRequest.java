package com.readora.commerce.dto;

import com.readora.commerce.entity.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Delivery type is chosen per item, not once for the whole order — a cart can mix physical and
 * virtual items (quick-commerce model: everything physical ships from one store at once,
 * virtual items are available instantly). shippingAddress is required only when at least one
 * item is PHYSICAL — validated in CheckoutService, not here, since it's a cross-field rule bean
 * validation doesn't express well. paymentMethod is "WALLET" or "UPI"; upiId is required only
 * for UPI.
 */
public record CheckoutRequest(
        @Valid ShippingAddress shippingAddress,
        @NotBlank String paymentMethod,
        String upiId,
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

    public record Item(@NotNull UUID bookId, int qty, @NotNull DeliveryType deliveryType) {
    }
}
