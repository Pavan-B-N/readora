package com.readora.commerce.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewOrderRequest(@NotBlank String note) {
}
