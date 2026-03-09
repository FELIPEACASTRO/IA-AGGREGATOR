package com.ia.aggregator.application.asset.usecase;

import com.ia.aggregator.application.asset.port.in.AgentUseCase;
import com.ia.aggregator.application.asset.port.out.AgentRepository;
import com.ia.aggregator.domain.asset.AgentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AgentUseCaseImpl implements AgentUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgentUseCaseImpl.class);

    private final AgentRepository agentRepository;

    public AgentUseCaseImpl(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public AgentDefinition create(AgentDefinition agent) {
        AgentDefinition withId = new AgentDefinition(
                agent.id() != null ? agent.id() : UUID.randomUUID(),
                agent.orgId(), agent.name(), agent.description(),
                agent.identity(), agent.instructions(), agent.defaultModel(),
                agent.knowledgeBaseId(), agent.permittedTools(),
                agent.maxBudgetUsd(), agent.requiresApproval(),
                agent.category(), agent.metadata()
        );
        agentRepository.save(withId);
        log.info("Agent created: id={}, name={}, category={}", withId.id(), withId.name(), withId.category());
        return withId;
    }

    @Override
    public AgentDefinition getById(UUID agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new NoSuchElementException("Agent not found: " + agentId));
    }

    @Override
    public List<AgentDefinition> listByOrg(UUID orgId) {
        return agentRepository.findByOrg(orgId);
    }

    @Override
    public List<AgentDefinition> listByCategory(UUID orgId, AgentDefinition.AgentCategory category) {
        return agentRepository.findByCategory(orgId, category);
    }

    @Override
    public List<AgentDefinition> listPrebuilt() {
        return agentRepository.findPrebuilt();
    }

    @Override
    public AgentDefinition update(AgentDefinition agent) {
        agentRepository.save(agent);
        log.info("Agent updated: id={}", agent.id());
        return agent;
    }

    @Override
    public void delete(UUID agentId) {
        agentRepository.delete(agentId);
        log.info("Agent deleted: id={}", agentId);
    }
}
