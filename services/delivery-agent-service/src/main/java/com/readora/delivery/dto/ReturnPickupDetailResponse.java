package com.readora.delivery.dto;

/** order reuses OrderDeliveryDetailResponse — a pickup needs exactly the same shipping-address/items detail a delivery does. */
public record ReturnPickupDetailResponse(ReturnPickupResponse pickup, OrderDeliveryDetailResponse order) {
}
