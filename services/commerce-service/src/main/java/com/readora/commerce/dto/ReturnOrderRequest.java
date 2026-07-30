package com.readora.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request body for returning a delivered order.
public record ReturnOrderRequest(@NotBlank @Size(max = 500) String reason) {
}
