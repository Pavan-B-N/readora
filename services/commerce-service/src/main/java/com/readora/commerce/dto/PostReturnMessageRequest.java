package com.readora.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostReturnMessageRequest(@NotBlank @Size(max = 2000) String content) {
}
