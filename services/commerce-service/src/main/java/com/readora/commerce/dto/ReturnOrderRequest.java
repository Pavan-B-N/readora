package com.readora.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnOrderRequest(@NotBlank @Size(max = 500) String reason) {
}
