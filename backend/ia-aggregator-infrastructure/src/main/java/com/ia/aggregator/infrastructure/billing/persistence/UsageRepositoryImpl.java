package com.ia.aggregator.infrastructure.billing.persistence;

import com.ia.aggregator.application.billing.port.out.UsageRepository;
import com.ia.aggregator.domain.billing.UsageRecord;
import com.ia.aggregator.infrastructure.billing.persistence.entity.UsageRecordJpaEntity;
import com.ia.aggregator.infrastructure.billing.persistence.repository.UsageRecordJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Component
public class UsageRepositoryImpl implements UsageRepository {

    private final UsageRecordJpaRepository jpa;

    public UsageRepositoryImpl(UsageRecordJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(UsageRecord record) {
        UsageRecordJpaEntity entity = new UsageRecordJpaEntity();
        entity.setId(record.id());
        entity.setOrgId(record.orgId());
        entity.setUserId(record.userId());
        entity.setMetric(record.capability());
        entity.setAmount(record.inputTokens() + record.outputTokens());
        entity.setUnit("tokens");
        entity.setModelUsed(record.model());
        entity.setProviderUsed(record.provider());
        entity.setPeriod(YearMonth.now().toString());
        entity.setRecordedAt(record.timestamp());
        jpa.save(entity);
    }

    @Override
    public List<UsageRecord> findByOrg(UUID orgId, Instant from, Instant to) {
        return jpa.findByOrgIdAndRecordedAtBetween(orgId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public double sumCostByOrg(UUID orgId, Instant from, Instant to) {
        long totalTokens = jpa.sumAmountByOrgIdAndRecordedAtBetween(orgId, from, to);
        // Approximate cost: $0.002 per 1k tokens (blended average)
        return totalTokens * 0.002 / 1000.0;
    }

    @Override
    public long countByOrg(UUID orgId, Instant from, Instant to) {
        return jpa.countByOrgIdAndRecordedAtBetween(orgId, from, to);
    }

    private UsageRecord toDomain(UsageRecordJpaEntity entity) {
        return new UsageRecord(
                entity.getId(), entity.getOrgId(), entity.getUserId(),
                entity.getMetric(), entity.getProviderUsed(), entity.getModelUsed(),
                (int) entity.getAmount(), 0, entity.getAmount() * 0.002 / 1000.0,
                entity.getRecordedAt()
        );
    }
}
