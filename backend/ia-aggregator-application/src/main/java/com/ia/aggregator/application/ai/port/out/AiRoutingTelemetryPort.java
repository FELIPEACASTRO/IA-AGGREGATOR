package com.ia.aggregator.application.ai.port.out;

public interface AiRoutingTelemetryPort {

    void recordAttempt(String model, String provider);

    void recordSuccess(String model, String provider, boolean fallbackUsed, int attempts);

    void recordFailure(String model, String provider, String errorCode);
}
