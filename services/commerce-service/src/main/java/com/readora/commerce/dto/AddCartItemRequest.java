package com.readora.commerce.dto;

import com.readora.commerce.entity.DeliveryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** storeId is the caller's currently-delivering-from store — required for PHYSICAL, ignored for VIRTUAL (store-independent). Validated in CartService, not here, since the requirement depends on deliveryType. */
public record AddCartItemRequest(@NotNull UUID bookId, @Min(1) int qty, @NotNull DeliveryType deliveryType, UUID storeId) {
}
