package com.readora.commerce.dto;

import jakarta.validation.constraints.Min;

public record SetCartItemRequest(@Min(0) int qty) {
}
