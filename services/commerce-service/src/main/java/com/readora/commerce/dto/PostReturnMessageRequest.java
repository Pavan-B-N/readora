package com.readora.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request body for posting a message to a return's chat thread.
public record PostReturnMessageRequest(@NotBlank @Size(max = 2000) String content) {
}
