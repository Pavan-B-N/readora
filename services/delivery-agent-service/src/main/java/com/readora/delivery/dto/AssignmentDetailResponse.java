package com.readora.delivery.dto;

// A delivery assignment plus the order's full delivery detail.
public record AssignmentDetailResponse(AssignmentResponse assignment, OrderDeliveryDetailResponse order) {
}
