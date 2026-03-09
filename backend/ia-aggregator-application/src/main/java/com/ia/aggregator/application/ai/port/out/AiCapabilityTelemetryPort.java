package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.domain.ai.Capability;

/**
 * Telemetry port for capability-aware metrics recording.
 *
 * <p>Extends the telemetry model to include the capability dimension,
 * enabling per-capability dashboards and alerting.
 *
 * <p>Design: Port (Hexagonal Architecture) — the infrastructure's
 * Micrometer adapter implements this to emit Prometheus metrics.
 */
public interface AiCapabilityTelemetryPort {

    /**
     * Records a provider invocation attempt.
     */
    void recordAttempt(Capability capability, String model, String provider);

    /**
     * Records a successful provider invocation.
     */
    void recordSuccess(Capability capability, String model, String provider,
                       boolean fallbackUsed, int attempts);

    /**
     * Records a failed provider invocation.
     */
    void recordFailure(Capability capability, String model, String provider, String errorCode);

    /**
     * Records a guardrail block for a capability request.
     */
    void recordGuardrailBlocked(Capability capability, String stage, String model,
                                String provider, String reason);

    /**
     * Records latency for a capability invocation in milliseconds.
     */
    void recordLatency(Capability capability, String provider, long durationMs);
}
