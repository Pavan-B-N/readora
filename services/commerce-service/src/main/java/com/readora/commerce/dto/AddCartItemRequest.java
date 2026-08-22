package com.readora.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequest(@NotNull UUID bookId, @Min(1) int qty) {
}
