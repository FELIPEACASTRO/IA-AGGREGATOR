package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.dto.ChatResponse;
import com.ia.aggregator.application.ai.port.in.ChatUseCase;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.application.ai.port.out.AiRoutingTelemetryPort;
import com.ia.aggregator.application.ai.port.out.ChatModelRoutingPolicy;
import com.ia.aggregator.application.ai.port.out.OutputGuardrailPort;
import com.ia.aggregator.application.ai.port.out.PromptGuardrailPort;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatUseCaseImpl implements ChatUseCase {

    private final List<AiModelProvider> providers;
    private final ChatModelRoutingPolicy routingPolicy;
    private final AiRoutingTelemetryPort telemetryPort;
    private final PromptGuardrailPort promptGuardrailPort;
    private final OutputGuardrailPort outputGuardrailPort;

    public ChatUseCaseImpl(List<AiModelProvider> providers,
                           ChatModelRoutingPolicy routingPolicy,
                           AiRoutingTelemetryPort telemetryPort,
                           PromptGuardrailPort promptGuardrailPort,
                           OutputGuardrailPort outputGuardrailPort) {
        this.providers = providers;
        this.routingPolicy = routingPolicy;
        this.telemetryPort = telemetryPort;
        this.promptGuardrailPort = promptGuardrailPort;
        this.outputGuardrailPort = outputGuardrailPort;
    }

    @Override
    public ChatResponse execute(ChatCommand command) {
        String requestId = UUID.randomUUID().toString();

        try {
            promptGuardrailPort.validate(command.prompt());
        } catch (BusinessException ex) {
            telemetryPort.recordGuardrailBlocked(
                    "prompt",
                    resolveModelTag(command.preferredModel()),
                    "pre_provider",
                    resolveGuardrailReason(ex)
            );
            throw ex;
        }

        List<String> orderedModels = routingPolicy.resolveOrderedModels(command);
        if (orderedModels.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_001, "No model available for routing");
        }

        int attempts = 0;
        boolean foundAtLeastOneProvider = false;
        TechnicalException lastTechnicalError = null;
        List<String> providerOrder = resolveProviderOrder(command);

        for (int modelIndex = 0; modelIndex < orderedModels.size(); modelIndex++) {
            String model = orderedModels.get(modelIndex);
            List<AiModelProvider> matchingProviders = providers.stream()
                    .filter(provider -> provider.supportsModel(model))
                    .filter(provider -> providerOrder.isEmpty() || providerOrder.contains(provider.providerId()))
                    .toList();

            if (matchingProviders.isEmpty()) {
                continue;
            }

            foundAtLeastOneProvider = true;

            for (AiModelProvider provider : sortProviders(matchingProviders, providerOrder)) {
                attempts++;
                telemetryPort.recordAttempt(model, provider.providerName());

                try {
                    AiPromptResponse providerResponse = provider.sendPrompt(new AiPromptRequest(
                            requestId,
                            command.prompt(),
                            command.provider(),
                            model,
                            command.systemPrompt(),
                            command.temperature(),
                            command.maxTokens(),
                            command.streamRequested(),
                            command.metadata() == null ? Map.of() : command.metadata(),
                            command.fallbackProviders() == null ? List.of() : command.fallbackProviders()
                    ));

                    try {
                        outputGuardrailPort.validate(providerResponse.content());
                    } catch (BusinessException ex) {
                        telemetryPort.recordGuardrailBlocked(
                                "output",
                                model,
                                provider.providerName(),
                                resolveGuardrailReason(ex)
                        );
                        throw ex;
                    }

                    boolean fallbackUsed = attempts > 1 || modelIndex > 0;
                    telemetryPort.recordSuccess(model, provider.providerName(), fallbackUsed, attempts);
                    telemetryPort.recordLatency(model, provider.providerName(), providerResponse.latencyMs());
                    telemetryPort.recordEstimatedCost(
                            model,
                            provider.providerName(),
                            providerResponse.estimatedCost().supported() ? providerResponse.estimatedCost().amount() : null,
                            providerResponse.estimatedCost().currency()
                    );
                    if (fallbackUsed) {
                        telemetryPort.recordFallback(model, provider.providerName());
                    }

                    return new ChatResponse(
                            providerResponse.content(),
                            providerResponse.modelUsed(),
                            providerResponse.providerUsed(),
                            fallbackUsed,
                            attempts,
                            providerResponse.requestId(),
                            providerResponse.usage(),
                            providerResponse.estimatedCost(),
                            providerResponse.latencyMs(),
                            providerResponse.finishReason()
                    );
                } catch (TechnicalException ex) {
                    lastTechnicalError = ex;
                    telemetryPort.recordFailure(model, provider.providerName(), ex.getErrorCode().getCode());
                }
            }
        }

        if (!foundAtLeastOneProvider) {
            throw new BusinessException(ErrorCode.AI_001, "No provider supports the requested models");
        }

        if (lastTechnicalError != null) {
            throw lastTechnicalError;
        }

        throw new TechnicalException(ErrorCode.AI_002, "All providers failed to generate response");
    }

    private static String resolveModelTag(String preferredModel) {
        return preferredModel == null || preferredModel.isBlank() ? "auto" : preferredModel;
    }

    private static String resolveGuardrailReason(BusinessException ex) {
        return ex.getErrorCode().getCode();
    }

    private static List<String> resolveProviderOrder(ChatCommand command) {
        List<String> providerOrder = new ArrayList<>();
        if (command.provider() != null && !command.provider().isBlank()) {
            providerOrder.add(command.provider().trim());
        }
        if (command.fallbackProviders() != null) {
            command.fallbackProviders().stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .filter(item -> !providerOrder.contains(item))
                    .forEach(providerOrder::add);
        }
        return providerOrder;
    }

    private static List<AiModelProvider> sortProviders(List<AiModelProvider> matchingProviders, List<String> providerOrder) {
        if (providerOrder.isEmpty()) {
            return matchingProviders;
        }

        return matchingProviders.stream()
                .sorted((left, right) -> Integer.compare(indexOfProvider(providerOrder, left.providerId()), indexOfProvider(providerOrder, right.providerId())))
                .toList();
    }

    private static int indexOfProvider(List<String> providerOrder, String providerId) {
        int index = providerOrder.indexOf(providerId);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}
