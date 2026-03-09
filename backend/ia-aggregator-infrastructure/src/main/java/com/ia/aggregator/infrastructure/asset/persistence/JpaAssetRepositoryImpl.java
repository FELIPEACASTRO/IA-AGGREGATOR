package com.ia.aggregator.infrastructure.asset.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.asset.port.out.AssetRepository;
import com.ia.aggregator.domain.asset.Asset;
import com.ia.aggregator.infrastructure.asset.persistence.entity.AssetJpaEntity;
import com.ia.aggregator.infrastructure.asset.persistence.repository.AssetJpaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Primary
@Component
public class JpaAssetRepositoryImpl implements AssetRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final AssetJpaRepository jpa;

    public JpaAssetRepositoryImpl(AssetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Asset asset) {
        AssetJpaEntity entity = jpa.findById(asset.id()).orElseGet(AssetJpaEntity::new);
        entity.setId(asset.id());
        entity.setOrgId(asset.orgId());
        entity.setCreatedBy(asset.createdBy());
        entity.setName(asset.name());
        entity.setDescription(asset.description());
        entity.setAssetType(asset.type().name());
        entity.setScope(asset.scope().name().toLowerCase());
        entity.setContent(asset.content());
        entity.setVariables(toJson(asset.variables()));
        entity.setMetadata(toJson(asset.metadata()));
        entity.setUsageCount(asset.usageCount());
        entity.setAverageRating(asset.averageRating());
        entity.setPublished(asset.published());
        jpa.save(entity);
    }

    @Override
    public Optional<Asset> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Asset> findByOrg(UUID orgId, Asset.AssetScope scope, Asset.AssetType type,
                                  int offset, int limit) {
        if (scope != null && type != null) {
            return jpa.findByOrgIdAndScopeAndAssetType(
                    orgId, scope.name().toLowerCase(), type.name(),
                    PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit))
            ).stream().map(this::toDomain).toList();
        }
        return jpa.findByOrgId(orgId, PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Asset> search(UUID orgId, String query, int offset, int limit) {
        return jpa.search(orgId, query, PageRequest.of(offset / Math.max(1, limit), Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public void incrementUsage(UUID id) {
        jpa.incrementUsageCount(id);
    }

    @Override
    public double getTotalCost(UUID id) {
        return jpa.findById(id).map(AssetJpaEntity::getTotalCostUsd).orElse(0.0);
    }

    @Override
    public double getSuccessRate(UUID id) {
        return jpa.findById(id).map(entity -> {
            long total = entity.getSuccessCount() + entity.getFailureCount();
            if (total == 0) return 1.0;
            return (double) entity.getSuccessCount() / total;
        }).orElse(1.0);
    }

    private Asset toDomain(AssetJpaEntity entity) {
        return new Asset(
                entity.getId(), entity.getOrgId(), entity.getCreatedBy(),
                entity.getName(), entity.getDescription(),
                Asset.AssetType.valueOf(entity.getAssetType()),
                Asset.AssetScope.valueOf(entity.getScope().toUpperCase()),
                entity.getContent(),
                fromJson(entity.getVariables()),
                fromJson(entity.getMetadata()),
                entity.getUsageCount(), entity.getAverageRating(),
                entity.isPublished(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map != null ? map : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
