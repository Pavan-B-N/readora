package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePublisherRequest(@NotBlank String name, @NotBlank String slug) {
}
