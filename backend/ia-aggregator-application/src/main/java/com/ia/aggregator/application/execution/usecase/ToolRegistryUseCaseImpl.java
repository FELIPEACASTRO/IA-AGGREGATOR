package com.ia.aggregator.application.execution.usecase;

import com.ia.aggregator.application.execution.port.in.ToolRegistryUseCase;
import com.ia.aggregator.application.execution.port.out.ToolRepository;
import com.ia.aggregator.domain.execution.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ToolRegistryUseCaseImpl implements ToolRegistryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryUseCaseImpl.class);

    private final ToolRepository toolRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ToolRegistryUseCaseImpl(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Override
    public ToolDefinition register(ToolDefinition tool) {
        ToolDefinition withId = new ToolDefinition(
                tool.id() != null ? tool.id() : UUID.randomUUID(),
                tool.name(), tool.description(), tool.schemaJson(),
                tool.scope(), tool.riskLevel(), tool.estimatedCostUsd(),
                tool.endpoint(), tool.headers(), tool.requiresApproval(), true
        );
        toolRepository.save(withId);
        log.info("Tool registered: id={}, name={}, scope={}, risk={}",
                withId.id(), withId.name(), withId.scope(), withId.riskLevel());
        return withId;
    }

    @Override
    public ToolDefinition getById(UUID toolId) {
        return toolRepository.findById(toolId)
                .orElseThrow(() -> new NoSuchElementException("Tool not found: " + toolId));
    }

    @Override
    public List<ToolDefinition> listAll() {
        return toolRepository.findAll();
    }

    @Override
    public List<ToolDefinition> listByScope(ToolDefinition.ToolScope scope) {
        return toolRepository.findByScope(scope);
    }

    @Override
    public ToolDefinition update(ToolDefinition tool) {
        toolRepository.save(tool);
        return tool;
    }

    @Override
    public void disable(UUID toolId) {
        ToolDefinition tool = getById(toolId);
        ToolDefinition disabled = new ToolDefinition(
                tool.id(), tool.name(), tool.description(), tool.schemaJson(),
                tool.scope(), tool.riskLevel(), tool.estimatedCostUsd(),
                tool.endpoint(), tool.headers(), tool.requiresApproval(), false
        );
        toolRepository.save(disabled);
        log.info("Tool disabled: id={}", toolId);
    }

    @Override
    public void enable(UUID toolId) {
        ToolDefinition tool = getById(toolId);
        ToolDefinition enabled = new ToolDefinition(
                tool.id(), tool.name(), tool.description(), tool.schemaJson(),
                tool.scope(), tool.riskLevel(), tool.estimatedCostUsd(),
                tool.endpoint(), tool.headers(), tool.requiresApproval(), true
        );
        toolRepository.save(enabled);
        log.info("Tool enabled: id={}", toolId);
    }

    @Override
    public void delete(UUID toolId) {
        toolRepository.delete(toolId);
        log.info("Tool deleted: id={}", toolId);
    }

    @Override
    public ToolInvocationResult invoke(UUID toolId, String inputJson) {
        ToolDefinition tool = getById(toolId);
        if (!tool.enabled()) {
            return new ToolInvocationResult(toolId, null, 0, 0, false, "Tool is disabled");
        }

        long start = System.currentTimeMillis();
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(tool.endpoint()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(inputJson));

            if (tool.headers() != null) {
                tool.headers().forEach(requestBuilder::header);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            long duration = System.currentTimeMillis() - start;
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            log.info("Tool invoked: id={}, name={}, status={}, duration={}ms",
                    toolId, tool.name(), response.statusCode(), duration);

            return new ToolInvocationResult(
                    toolId, response.body(), duration, tool.estimatedCostUsd(),
                    success, success ? null : "HTTP " + response.statusCode()
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Tool invocation failed: id={}, error={}", toolId, e.getMessage());
            return new ToolInvocationResult(toolId, null, duration, 0, false, e.getMessage());
        }
    }
}
