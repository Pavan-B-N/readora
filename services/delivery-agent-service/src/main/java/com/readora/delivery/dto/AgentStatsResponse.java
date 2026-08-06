package com.readora.delivery.dto;

import java.math.BigDecimal;

// The caller's lifetime completed-work and earnings stats.
public record AgentStatsResponse(
        int completedDeliveries,
        int completedReturnPickups,
        BigDecimal totalEarnings,
        String currency
) {
}
