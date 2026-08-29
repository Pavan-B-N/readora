package com.readora.delivery.dto;

/** One line item, snapshotted at assignment-creation time — same field names as commerce-service's OrderDeliveryDetailResponse.Item, so it serializes straight through with no manual mapping. */
public record ItemSnapshot(String title, int qty) {
}
