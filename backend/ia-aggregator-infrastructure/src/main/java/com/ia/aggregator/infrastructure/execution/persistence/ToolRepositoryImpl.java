package com.ia.aggregator.infrastructure.execution.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.execution.port.out.ToolRepository;
import com.ia.aggregator.domain.execution.ToolDefinition;
import com.ia.aggregator.infrastructure.execution.persistence.entity.ToolJpaEntity;
import com.ia.aggregator.infrastructure.execution.persistence.repository.ToolJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ToolRepositoryImpl implements ToolRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final ToolJpaRepository jpa;

    public ToolRepositoryImpl(ToolJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ToolDefinition tool) {
        ToolJpaEntity entity = jpa.findById(tool.id()).orElseGet(ToolJpaEntity::new);
        entity.setId(tool.id());
        entity.setName(tool.name());
        entity.setDescription(tool.description());
        entity.setSchemaJson(tool.schemaJson());
        entity.setScope(tool.scope().name());
        entity.setRiskLevel(tool.riskLevel().name());
        entity.setEstimatedCostUsd(java.math.BigDecimal.valueOf(tool.estimatedCostUsd()));
        entity.setEndpoint(tool.endpoint());
        entity.setHeaders(toJson(tool.headers()));
        entity.setRequiresApproval(tool.requiresApproval());
        entity.setEnabled(tool.enabled());
        jpa.save(entity);
    }

    @Override
    public Optional<ToolDefinition> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<ToolDefinition> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ToolDefinition> findByScope(ToolDefinition.ToolScope scope) {
        return jpa.findByScope(scope.name()).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    private ToolDefinition toDomain(ToolJpaEntity entity) {
        return new ToolDefinition(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getSchemaJson(),
                ToolDefinition.ToolScope.valueOf(entity.getScope()),
                ToolDefinition.ToolRiskLevel.valueOf(entity.getRiskLevel()),
                entity.getEstimatedCostUsd().doubleValue(),
                entity.getEndpoint(),
                fromJson(entity.getHeaders()),
                entity.isRequiresApproval(),
                entity.isEnabled()
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
