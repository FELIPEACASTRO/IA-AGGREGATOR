package com.ia.aggregator.application.ai.usecase;

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

import java.util.List;

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

        for (int modelIndex = 0; modelIndex < orderedModels.size(); modelIndex++) {
            String model = orderedModels.get(modelIndex);
            List<AiModelProvider> matchingProviders = providers.stream()
                    .filter(provider -> provider.supports(model))
                    .toList();

            if (matchingProviders.isEmpty()) {
                continue;
            }

            foundAtLeastOneProvider = true;

            for (AiModelProvider provider : matchingProviders) {
                attempts++;
                telemetryPort.recordAttempt(model, provider.providerName());

                try {
                    String content = provider.generate(command.prompt(), model);
                    try {
                        outputGuardrailPort.validate(content);
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
                    return new ChatResponse(content, model, provider.providerName(), fallbackUsed, attempts);
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
}
