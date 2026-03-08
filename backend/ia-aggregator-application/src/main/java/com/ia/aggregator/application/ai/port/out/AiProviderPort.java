package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.dto.AiCostEstimate;
import com.ia.aggregator.application.ai.dto.AiHealthStatus;
import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import com.ia.aggregator.application.ai.dto.AiStreamEvent;
import com.ia.aggregator.application.ai.dto.AiUsageEstimate;
import com.ia.aggregator.common.exception.AiGatewayException;
import com.ia.aggregator.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public interface AiProviderPort {

    String providerName();

    boolean supports(String model);

    String generate(String prompt, String model);

    default String providerId() {
        return providerName();
    }

    default boolean supportsModel(String model) {
        return supports(model);
    }

    default boolean isConfigured() {
        return true;
    }

    default List<String> requiredSecrets() {
        return List.of();
    }

    default List<String> supportedModels() {
        return List.of();
    }

    default boolean supportsStreaming() {
        return false;
    }

    default String defaultModel() {
        return supportedModels().isEmpty() ? null : supportedModels().getFirst();
    }

    default AiPromptResponse sendPrompt(AiPromptRequest request) {
        String resolvedModel = request.model() == null || request.model().isBlank() ? defaultModel() : request.model();
        String content = generate(request.prompt(), resolvedModel);
        return new AiPromptResponse(content, providerName(), resolvedModel, request.requestId());
    }

    default void streamPrompt(AiPromptRequest request, Consumer<AiStreamEvent> eventConsumer) {
        throw new AiGatewayException(
                ErrorCode.AI_005,
                request.requestId(),
                providerId(),
                request.model(),
                false,
                "Streaming nao suportado para o provider " + providerName()
        );
    }

    default AiHealthStatus healthCheck() {
        return new AiHealthStatus(
                providerId(),
                providerName(),
                isConfigured(),
                true,
                isConfigured(),
                "UNKNOWN",
                Instant.now(),
                null,
                null,
                supportsStreaming(),
                defaultModel(),
                supportedModels(),
                requiredSecrets()
        );
    }

    default AiCostEstimate estimateCost(AiPromptRequest request, AiUsageEstimate usage) {
        return AiCostEstimate.unsupported(providerId(), request.model());
    }
}
