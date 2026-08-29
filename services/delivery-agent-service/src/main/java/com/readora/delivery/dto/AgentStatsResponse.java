package com.readora.delivery.dto;

import java.math.BigDecimal;

public record AgentStatsResponse(
        int completedDeliveries,
        int completedReturnPickups,
        BigDecimal totalEarnings,
        String currency
) {
}
