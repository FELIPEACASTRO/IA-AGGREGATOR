package com.ia.aggregator.infrastructure.ai.telemetry;

import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.domain.ai.Capability;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer-based implementation of capability-aware telemetry.
 *
 * <p>Emits Prometheus-compatible metrics with the {@code capability} dimension:
 * <ul>
 *   <li>{@code ai.capability.attempts} — counter per capability/model/provider</li>
 *   <li>{@code ai.capability.success} — counter per capability/model/provider</li>
 *   <li>{@code ai.capability.failures} — counter per capability/model/provider/errorCode</li>
 *   <li>{@code ai.capability.guardrail.blocked} — counter per capability/stage/reason</li>
 *   <li>{@code ai.capability.latency} — timer per capability/provider</li>
 * </ul>
 *
 * <p>Design: Adapter (Hexagonal Architecture) — bridges application port to Micrometer.
 * <p>Big O: O(1) per metric recording (Micrometer internal lookup is O(1) amortized).
 */
@Component
public class MicrometerAiCapabilityTelemetryAdapter implements AiCapabilityTelemetryPort {

    private final MeterRegistry meterRegistry;

    public MicrometerAiCapabilityTelemetryAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordAttempt(Capability capability, String model, String provider) {
        meterRegistry.counter(
                "ai.capability.attempts",
                "capability", capability.name(),
                "model", model,
                "provider", provider
        ).increment();
    }

    @Override
    public void recordSuccess(Capability capability, String model, String provider,
                              boolean fallbackUsed, int attempts) {
        meterRegistry.counter(
                "ai.capability.success",
                "capability", capability.name(),
                "model", model,
                "provider", provider,
                "fallback", String.valueOf(fallbackUsed)
        ).increment();

        meterRegistry.summary(
                "ai.capability.attempts.before.success",
                "capability", capability.name()
        ).record(attempts);
    }

    @Override
    public void recordFailure(Capability capability, String model, String provider, String errorCode) {
        meterRegistry.counter(
                "ai.capability.failures",
                "capability", capability.name(),
                "model", model,
                "provider", provider,
                "errorCode", errorCode
        ).increment();
    }

    @Override
    public void recordGuardrailBlocked(Capability capability, String stage, String model,
                                       String provider, String reason) {
        meterRegistry.counter(
                "ai.capability.guardrail.blocked",
                "capability", capability.name(),
                "stage", stage,
                "model", model,
                "provider", provider,
                "reason", reason
        ).increment();
    }

    @Override
    public void recordLatency(Capability capability, String provider, long durationMs) {
        Timer.builder("ai.capability.latency")
                .tag("capability", capability.name())
                .tag("provider", provider)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records a provider health status change.
     * Emits {@code ai.provider.health} gauge (1 = UP, 0 = DOWN, 0.5 = DEGRADED).
     */
    public void recordHealthStatus(String provider, String status) {
        double value = switch (status) {
            case "UP" -> 1.0;
            case "DEGRADED" -> 0.5;
            case "DOWN" -> 0.0;
            default -> -1.0;
        };
        meterRegistry.gauge(
                "ai.provider.health",
                io.micrometer.core.instrument.Tags.of("provider", provider),
                value
        );
    }

    /**
     * Records estimated cost per request for FinOps tracking.
     */
    public void recordCostEstimate(Capability capability, String provider, double estimatedCostUsd) {
        meterRegistry.counter(
                "ai.capability.cost.estimate.usd",
                "capability", capability.name(),
                "provider", provider
        ).increment(estimatedCostUsd);
    }
}
