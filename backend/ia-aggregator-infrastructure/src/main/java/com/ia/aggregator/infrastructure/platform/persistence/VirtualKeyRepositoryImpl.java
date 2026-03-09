package com.ia.aggregator.infrastructure.platform.persistence;

import com.ia.aggregator.application.platform.port.out.VirtualKeyRepository;
import com.ia.aggregator.domain.platform.VirtualKey;
import com.ia.aggregator.infrastructure.platform.persistence.entity.VirtualKeyJpaEntity;
import com.ia.aggregator.infrastructure.platform.persistence.repository.VirtualKeyJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VirtualKeyRepositoryImpl implements VirtualKeyRepository {

    private final VirtualKeyJpaRepository jpa;

    public VirtualKeyRepositoryImpl(VirtualKeyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(VirtualKey key) {
        VirtualKeyJpaEntity entity = new VirtualKeyJpaEntity();
        entity.setId(key.id());
        entity.setOrgId(key.orgId());
        entity.setCreatedBy(key.createdBy());
        entity.setName(key.name());
        entity.setKeyHash(key.keyHash());
        entity.setKeyPrefix(key.keyPrefix());
        entity.setAllowedModels(key.allowedModels() != null ? key.allowedModels().toArray(new String[0]) : new String[0]);
        entity.setAllowedCapabilities(key.allowedCapabilities() != null ? key.allowedCapabilities().toArray(new String[0]) : new String[0]);
        entity.setRateLimitRpm(key.rateLimitRpm());
        entity.setBudgetLimitUsd(java.math.BigDecimal.valueOf(key.budgetLimitUsd()));
        entity.setSpentUsd(java.math.BigDecimal.valueOf(key.spentUsd()));
        entity.setEnabled(key.enabled());
        entity.setCreatedAt(key.createdAt());
        entity.setLastUsedAt(key.lastUsedAt());
        entity.setExpiresAt(key.expiresAt());
        jpa.save(entity);
    }

    @Override
    public Optional<VirtualKey> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<VirtualKey> findByKeyHash(String keyHash) {
        return jpa.findByKeyHash(keyHash).map(this::toDomain);
    }

    @Override
    public List<VirtualKey> findByOrg(UUID orgId) {
        return jpa.findByOrgId(orgId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    private VirtualKey toDomain(VirtualKeyJpaEntity entity) {
        return new VirtualKey(
                entity.getId(),
                entity.getOrgId(),
                entity.getCreatedBy(),
                entity.getName(),
                entity.getKeyHash(),
                entity.getKeyPrefix(),
                entity.getAllowedModels() != null ? Arrays.asList(entity.getAllowedModels()) : List.of(),
                entity.getAllowedCapabilities() != null ? Arrays.asList(entity.getAllowedCapabilities()) : List.of(),
                entity.getRateLimitRpm(),
                entity.getBudgetLimitUsd().doubleValue(),
                entity.getSpentUsd().doubleValue(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getLastUsedAt(),
                entity.getExpiresAt()
        );
    }
}
