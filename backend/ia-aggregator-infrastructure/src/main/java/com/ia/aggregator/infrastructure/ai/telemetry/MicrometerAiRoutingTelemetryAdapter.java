package com.ia.aggregator.infrastructure.ai.telemetry;

import com.ia.aggregator.application.ai.port.out.AiRoutingTelemetryPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerAiRoutingTelemetryAdapter implements AiRoutingTelemetryPort {

    private final MeterRegistry meterRegistry;

    public MicrometerAiRoutingTelemetryAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordAttempt(String model, String provider) {
        meterRegistry.counter("ai.routing.attempts", "model", model, "provider", provider).increment();
    }

    @Override
    public void recordSuccess(String model, String provider, boolean fallbackUsed, int attempts) {
        meterRegistry.counter(
                "ai.routing.success",
                "model", model,
                "provider", provider,
                "fallback", String.valueOf(fallbackUsed)
        ).increment();

        meterRegistry.summary("ai.routing.attempts.before.success").record(attempts);
    }

    @Override
    public void recordFailure(String model, String provider, String errorCode) {
        meterRegistry.counter(
                "ai.routing.failures",
                "model", model,
                "provider", provider,
                "errorCode", errorCode
        ).increment();
    }

    @Override
    public void recordGuardrailBlocked(String stage, String model, String provider, String reason) {
        meterRegistry.counter(
                "ai.guardrail.blocked",
                "stage", stage,
                "model", model,
                "provider", provider,
                "reason", reason
        ).increment();
    }
}
