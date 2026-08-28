package com.readora.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** storeId is nullable — a guest who hasn't picked a store yet still gets virtual-only recommendations. */
public record ChatRequest(
        String conversationId,
        @NotBlank @Size(max = 4000) String message,
        String storeId
) {
}
