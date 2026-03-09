package com.ia.aggregator.infrastructure.execution.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ia.aggregator.application.execution.port.out.AgentExecutionRepository;
import com.ia.aggregator.domain.execution.AgentExecution;
import com.ia.aggregator.domain.execution.AutonomyLevel;
import com.ia.aggregator.infrastructure.execution.persistence.entity.AgentExecutionJpaEntity;
import com.ia.aggregator.infrastructure.execution.persistence.repository.AgentExecutionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentExecutionRepositoryImpl implements AgentExecutionRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final TypeReference<List<AgentExecution.AgentStep>> STEPS_TYPE = new TypeReference<>() {};

    private final AgentExecutionJpaRepository jpa;

    public AgentExecutionRepositoryImpl(AgentExecutionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(AgentExecution execution) {
        AgentExecutionJpaEntity entity = jpa.findById(execution.id()).orElseGet(AgentExecutionJpaEntity::new);
        entity.setId(execution.id());
        entity.setAgentDefinitionId(execution.agentDefinitionId());
        entity.setOrgId(execution.orgId());
        entity.setUserId(execution.userId());
        entity.setObjective(execution.objective());
        entity.setAutonomyLevel(execution.autonomyLevel().name());
        entity.setStatus(execution.status().name());
        entity.setSteps(toJson(execution.steps()));
        entity.setTotalCostUsd(java.math.BigDecimal.valueOf(execution.totalCostUsd()));
        entity.setMaxBudgetUsd(java.math.BigDecimal.valueOf(execution.maxBudgetUsd()));
        entity.setFinalAnswer(execution.finalAnswer());
        entity.setErrorMessage(execution.errorMessage());
        entity.setStartedAt(execution.startedAt());
        entity.setCompletedAt(execution.completedAt());
        jpa.save(entity);
    }

    @Override
    public Optional<AgentExecution> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<AgentExecution> findByOrg(UUID orgId, int offset, int limit) {
        int safeLimit = Math.max(1, limit);
        return jpa.findByOrgId(orgId, PageRequest.of(offset / safeLimit, safeLimit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    private AgentExecution toDomain(AgentExecutionJpaEntity entity) {
        return new AgentExecution(
                entity.getId(),
                entity.getAgentDefinitionId(),
                entity.getOrgId(),
                entity.getUserId(),
                entity.getObjective(),
                AutonomyLevel.valueOf(entity.getAutonomyLevel()),
                AgentExecution.AgentStatus.valueOf(entity.getStatus()),
                fromJson(entity.getSteps()),
                entity.getTotalCostUsd().doubleValue(),
                entity.getMaxBudgetUsd().doubleValue(),
                entity.getFinalAnswer(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj != null ? obj : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<AgentExecution.AgentStep> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, STEPS_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }
}
