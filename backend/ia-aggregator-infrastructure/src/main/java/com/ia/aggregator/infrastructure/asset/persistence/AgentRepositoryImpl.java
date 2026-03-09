package com.ia.aggregator.infrastructure.asset.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.asset.port.out.AgentRepository;
import com.ia.aggregator.domain.asset.AgentDefinition;
import com.ia.aggregator.infrastructure.asset.persistence.entity.AgentJpaEntity;
import com.ia.aggregator.infrastructure.asset.persistence.repository.AgentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AgentRepositoryImpl implements AgentRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final AgentJpaRepository jpa;

    public AgentRepositoryImpl(AgentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(AgentDefinition agent) {
        AgentJpaEntity entity = jpa.findById(agent.id()).orElseGet(AgentJpaEntity::new);
        entity.setId(agent.id());
        entity.setOrgId(agent.orgId());
        entity.setAgentId(agent.identity());
        entity.setName(agent.name());
        entity.setDescription(agent.description());
        entity.setCategory(agent.category() != null ? agent.category().name() : "CUSTOM");
        entity.setDefaultModel(agent.defaultModel());
        entity.setSystemPrompt(agent.instructions());
        entity.setCapabilities(agent.permittedTools() != null
                ? agent.permittedTools().toArray(new String[0])
                : new String[0]);
        entity.setActive(true);

        // Store knowledgeBaseId, maxBudgetUsd, requiresApproval and metadata in config jsonb
        Map<String, String> config = new HashMap<>(agent.metadata() != null ? agent.metadata() : Map.of());
        if (agent.knowledgeBaseId() != null) {
            config.put("knowledgeBaseId", agent.knowledgeBaseId().toString());
        }
        config.put("maxBudgetUsd", String.valueOf(agent.maxBudgetUsd()));
        config.put("requiresApproval", String.valueOf(agent.requiresApproval()));
        entity.setConfig(toJson(config));

        jpa.save(entity);
    }

    @Override
    public Optional<AgentDefinition> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<AgentDefinition> findByOrg(UUID orgId) {
        return jpa.findByOrgId(orgId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AgentDefinition> findByCategory(UUID orgId, AgentDefinition.AgentCategory category) {
        return jpa.findByOrgIdAndCategory(orgId, category.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AgentDefinition> findPrebuilt() {
        return jpa.findByIsPrebuiltTrueAndIsActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    private AgentDefinition toDomain(AgentJpaEntity entity) {
        Map<String, String> config = fromJson(entity.getConfig());

        UUID knowledgeBaseId = null;
        if (config.containsKey("knowledgeBaseId")) {
            try {
                knowledgeBaseId = UUID.fromString(config.get("knowledgeBaseId"));
            } catch (IllegalArgumentException ignored) {}
        }

        double maxBudgetUsd = 0.0;
        if (config.containsKey("maxBudgetUsd")) {
            try {
                maxBudgetUsd = Double.parseDouble(config.get("maxBudgetUsd"));
            } catch (NumberFormatException ignored) {}
        }

        boolean requiresApproval = Boolean.parseBoolean(config.getOrDefault("requiresApproval", "false"));

        // Build metadata without internal config keys
        Map<String, String> metadata = new HashMap<>(config);
        metadata.remove("knowledgeBaseId");
        metadata.remove("maxBudgetUsd");
        metadata.remove("requiresApproval");

        AgentDefinition.AgentCategory category;
        try {
            category = AgentDefinition.AgentCategory.valueOf(entity.getCategory());
        } catch (Exception e) {
            category = AgentDefinition.AgentCategory.CUSTOM;
        }

        List<String> permittedTools = entity.getCapabilities() != null
                ? Arrays.asList(entity.getCapabilities())
                : List.of();

        return new AgentDefinition(
                entity.getId(),
                entity.getOrgId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAgentId(),
                entity.getSystemPrompt(),
                entity.getDefaultModel(),
                knowledgeBaseId,
                permittedTools,
                maxBudgetUsd,
                requiresApproval,
                category,
                metadata
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
