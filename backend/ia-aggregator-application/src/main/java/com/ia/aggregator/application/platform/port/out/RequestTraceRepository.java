package com.ia.aggregator.application.platform.port.out;

import com.ia.aggregator.domain.platform.RequestTrace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RequestTraceRepository {
    void save(RequestTrace trace);
    List<RequestTrace> findByOrg(UUID orgId, Instant from, Instant to, int offset, int limit);
    List<RequestTrace> findByVirtualKey(UUID virtualKeyId, Instant from, Instant to, int offset, int limit);
    long countByOrg(UUID orgId, Instant from, Instant to);
    long countSuccessByOrg(UUID orgId, Instant from, Instant to);
    double sumCostByOrg(UUID orgId, Instant from, Instant to);
}
