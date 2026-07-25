package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

// Admin request to create a new publisher.
public record CreatePublisherRequest(@NotBlank String name, @NotBlank String slug) {
}
