package com.ia.aggregator.application.platform.port.in;

import com.ia.aggregator.domain.platform.RequestTrace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case for request tracing and observability.
 */
public interface RequestTracingUseCase {

    void record(RequestTrace trace);

    List<RequestTrace> listByOrg(UUID orgId, Instant from, Instant to, int page, int size);

    List<RequestTrace> listByVirtualKey(UUID virtualKeyId, Instant from, Instant to,
                                         int page, int size);

    TracingSummary getSummary(UUID orgId, Instant from, Instant to);

    record TracingSummary(long totalRequests, long successRequests, long failedRequests,
                           long cacheHits, double totalCostUsd, double avgLatencyMs,
                           double p95LatencyMs, double p99LatencyMs) {}
}
