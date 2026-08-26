package com.readora.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpRequest(
        @NotNull @DecimalMin(value = "1.00") @DecimalMax(value = "50000.00") BigDecimal amount
) {
}
