package com.readora.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        String conversationId,
        @NotBlank @Size(max = 4000) String message
) {
}
