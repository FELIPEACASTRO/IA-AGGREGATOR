package com.ia.aggregator.infrastructure.platform.persistence;

import com.ia.aggregator.application.platform.port.out.BatchJobRepository;
import com.ia.aggregator.domain.platform.BatchJob;
import com.ia.aggregator.infrastructure.platform.persistence.entity.BatchJobJpaEntity;
import com.ia.aggregator.infrastructure.platform.persistence.repository.BatchJobJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BatchJobRepositoryImpl implements BatchJobRepository {

    private final BatchJobJpaRepository jpa;

    public BatchJobRepositoryImpl(BatchJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(BatchJob job) {
        BatchJobJpaEntity entity = new BatchJobJpaEntity();
        entity.setId(job.id());
        entity.setOrgId(job.orgId());
        entity.setWebhookUrl(job.webhookUrl());
        entity.setTotalRequests(job.totalRequests());
        entity.setCompletedRequests(job.completedRequests());
        entity.setFailedRequests(job.failedRequests());
        entity.setStatus(job.status().name());
        entity.setTotalCostUsd(java.math.BigDecimal.valueOf(job.totalCostUsd()));
        entity.setDiscountPercent(java.math.BigDecimal.valueOf(job.discountPercent()));
        entity.setCreatedAt(job.createdAt());
        entity.setCompletedAt(job.completedAt());
        jpa.save(entity);
    }

    @Override
    public Optional<BatchJob> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<BatchJob> findByOrg(UUID orgId, int offset, int limit) {
        PageRequest page = PageRequest.of(offset / Math.max(limit, 1), limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpa.findByOrgId(orgId, page).stream()
                .map(this::toDomain)
                .toList();
    }

    private BatchJob toDomain(BatchJobJpaEntity entity) {
        return new BatchJob(
                entity.getId(),
                entity.getOrgId(),
                entity.getWebhookUrl(),
                entity.getTotalRequests(),
                entity.getCompletedRequests(),
                entity.getFailedRequests(),
                BatchJob.BatchStatus.valueOf(entity.getStatus()),
                entity.getTotalCostUsd().doubleValue(),
                entity.getDiscountPercent().doubleValue(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
