package com.readora.commerce.dto;

import jakarta.validation.constraints.Min;

// Request to set a cart item's quantity directly (0 removes it).
public record SetCartItemRequest(@Min(0) int qty) {
}
