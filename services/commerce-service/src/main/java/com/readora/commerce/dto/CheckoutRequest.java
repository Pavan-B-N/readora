package com.readora.commerce.dto;

import com.readora.commerce.entity.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// Delivery type is chosen per item since a cart can mix physical and virtual (quick-commerce: physical ships from one store, virtual is instant); shippingAddress is required only when an item is PHYSICAL, validated in CheckoutService since it's a cross-field rule bean validation can't express; paymentMethod is "WALLET" or "UPI", upiId required only for UPI.
public record CheckoutRequest(
        @Valid ShippingAddress shippingAddress,
        @NotBlank String paymentMethod,
        String upiId,
        @NotEmpty List<Item> items
) {
    // Snapshot of the recipient's shipping address for a PHYSICAL order.
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

    // One requested line item and how it should be delivered.
    public record Item(@NotNull UUID bookId, int qty, @NotNull DeliveryType deliveryType) {
    }
}
