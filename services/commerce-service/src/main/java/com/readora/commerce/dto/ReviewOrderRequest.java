package com.readora.commerce.dto;

import jakarta.validation.constraints.NotBlank;

/** decision is "APPROVE" or "REJECT" for a return awaiting review; null for a plain cancellation note (nothing to decide). */
public record ReviewOrderRequest(@NotBlank String note, String decision) {
}
