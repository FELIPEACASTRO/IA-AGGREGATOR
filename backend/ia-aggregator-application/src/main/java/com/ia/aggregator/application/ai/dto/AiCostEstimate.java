package com.ia.aggregator.application.ai.dto;

import java.math.BigDecimal;

public record AiCostEstimate(
        String providerId,
        String model,
        String currency,
        BigDecimal amount,
        boolean supported
) {
    public static AiCostEstimate unsupported(String providerId, String model) {
        return new AiCostEstimate(providerId, model, "USD", null, false);
    }
}
