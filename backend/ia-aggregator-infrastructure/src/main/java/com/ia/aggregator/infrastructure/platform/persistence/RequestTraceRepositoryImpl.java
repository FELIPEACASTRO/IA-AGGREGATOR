package com.ia.aggregator.infrastructure.platform.persistence;

import com.ia.aggregator.application.platform.port.out.RequestTraceRepository;
import com.ia.aggregator.domain.platform.RequestTrace;
import com.ia.aggregator.infrastructure.platform.persistence.entity.RequestTraceJpaEntity;
import com.ia.aggregator.infrastructure.platform.persistence.repository.RequestTraceJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class RequestTraceRepositoryImpl implements RequestTraceRepository {

    private final RequestTraceJpaRepository jpa;

    public RequestTraceRepositoryImpl(RequestTraceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(RequestTrace trace) {
        RequestTraceJpaEntity entity = new RequestTraceJpaEntity();
        entity.setId(trace.id());
        entity.setOrgId(trace.orgId());
        entity.setVirtualKeyId(trace.virtualKeyId());
        entity.setCapability(trace.capability());
        entity.setRequestedModel(trace.requestedModel());
        entity.setUsedModel(trace.usedModel());
        entity.setProvider(trace.provider());
        entity.setStatusCode(trace.statusCode());
        entity.setInputTokens(trace.inputTokens());
        entity.setOutputTokens(trace.outputTokens());
        entity.setCostUsd(java.math.BigDecimal.valueOf(trace.costUsd()));
        entity.setTotalLatencyMs(trace.totalLatencyMs());
        entity.setProviderLatencyMs(trace.providerLatencyMs());
        entity.setRoutingLatencyMs(trace.routingLatencyMs());
        entity.setCacheHit(trace.cacheHit());
        entity.setFallbackUsed(trace.fallbackUsed());
        entity.setErrorCode(trace.errorCode());
        entity.setErrorMessage(trace.errorMessage());
        entity.setTimestamp(trace.timestamp());
        jpa.save(entity);
    }

    @Override
    public List<RequestTrace> findByOrg(UUID orgId, Instant from, Instant to, int offset, int limit) {
        PageRequest page = PageRequest.of(offset / Math.max(limit, 1), limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return jpa.findByOrgIdAndTimestampBetween(orgId, from, to, page).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RequestTrace> findByVirtualKey(UUID virtualKeyId, Instant from, Instant to, int offset, int limit) {
        PageRequest page = PageRequest.of(offset / Math.max(limit, 1), limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return jpa.findByVirtualKeyIdAndTimestampBetween(virtualKeyId, from, to, page).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByOrg(UUID orgId, Instant from, Instant to) {
        return jpa.countByOrgIdAndTimestampBetween(orgId, from, to);
    }

    @Override
    public long countSuccessByOrg(UUID orgId, Instant from, Instant to) {
        return jpa.countSuccessByOrgIdAndTimestampBetween(orgId, from, to);
    }

    @Override
    public double sumCostByOrg(UUID orgId, Instant from, Instant to) {
        return jpa.sumCostByOrgIdAndTimestampBetween(orgId, from, to);
    }

    private RequestTrace toDomain(RequestTraceJpaEntity entity) {
        return new RequestTrace(
                entity.getId(),
                entity.getOrgId(),
                entity.getVirtualKeyId(),
                entity.getCapability(),
                entity.getRequestedModel(),
                entity.getUsedModel(),
                entity.getProvider(),
                entity.getStatusCode(),
                entity.getInputTokens(),
                entity.getOutputTokens(),
                entity.getCostUsd().doubleValue(),
                entity.getTotalLatencyMs(),
                entity.getProviderLatencyMs(),
                entity.getRoutingLatencyMs(),
                entity.isCacheHit(),
                entity.isFallbackUsed(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getTimestamp()
        );
    }
}
