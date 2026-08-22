package com.readora.catalog.dto;

import com.readora.catalog.entity.VirtualFileFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpsertVirtualEditionRequest(
        @NotBlank String fileUrl,
        @NotNull VirtualFileFormat fileFormat,
        Long fileSizeBytes,
        @NotNull BigDecimal price,
        @NotBlank String currency
) {
}
