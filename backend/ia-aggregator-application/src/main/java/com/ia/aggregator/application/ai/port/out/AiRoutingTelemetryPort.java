package com.ia.aggregator.application.ai.port.out;

import java.math.BigDecimal;

public interface AiRoutingTelemetryPort {

    void recordAttempt(String model, String provider);

    void recordSuccess(String model, String provider, boolean fallbackUsed, int attempts);

    void recordFailure(String model, String provider, String errorCode);

    void recordGuardrailBlocked(String stage, String model, String provider, String reason);

    default void recordLatency(String model, String provider, long latencyMs) {
    }

    default void recordFallback(String model, String provider) {
    }

    default void recordEstimatedCost(String model, String provider, BigDecimal amount, String currency) {
    }
}
