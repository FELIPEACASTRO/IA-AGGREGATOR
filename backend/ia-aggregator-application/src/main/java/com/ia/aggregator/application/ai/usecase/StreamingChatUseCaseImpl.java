package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.port.in.StreamingChatUseCase;
import com.ia.aggregator.application.ai.port.out.*;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Streaming chat implementation using SSE.
 *
 * <p>Resolves the best provider (with fallback), validates guardrails,
 * and returns a token-by-token Flux for SSE delivery.
 */
@Service
public class StreamingChatUseCaseImpl implements StreamingChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatUseCaseImpl.class);

    private final List<AiModelProvider> providers;
    private final ChatModelRoutingPolicy routingPolicy;
    private final PromptGuardrailPort promptGuardrailPort;

    public StreamingChatUseCaseImpl(List<AiModelProvider> providers,
                                     ChatModelRoutingPolicy routingPolicy,
                                     PromptGuardrailPort promptGuardrailPort) {
        this.providers = providers;
        this.routingPolicy = routingPolicy;
        this.promptGuardrailPort = promptGuardrailPort;
    }

    @Override
    public Flux<String> executeStreaming(ChatCommand command) {
        // Validate prompt guardrails synchronously before streaming
        promptGuardrailPort.validate(command.prompt());

        List<String> orderedModels = routingPolicy.resolveOrderedModels(command);
        if (orderedModels.isEmpty()) {
            return Flux.error(new BusinessException(ErrorCode.AI_001, "No model available for routing"));
        }

        // Find the first provider that supports streaming
        for (String model : orderedModels) {
            for (AiModelProvider provider : providers) {
                if (provider.supports(model) && provider instanceof StreamingCapableProvider streaming) {
                    log.debug("Streaming via {} with model {}", provider.providerName(), model);
                    return streaming.generateStream(command.prompt(), model)
                            .onErrorResume(e -> {
                                log.warn("Streaming failed for {}/{}: {}",
                                        provider.providerName(), model, e.getMessage());
                                return Flux.error(new TechnicalException(
                                        ErrorCode.AI_005,
                                        "Streaming failed: " + e.getMessage(), e));
                            });
                }
            }
        }

        // No streaming-capable provider found — fallback to non-streaming as a Flux of one
        for (String model : orderedModels) {
            for (AiModelProvider provider : providers) {
                if (provider.supports(model)) {
                    return Flux.defer(() -> {
                        try {
                            String content = provider.generate(command.prompt(), model);
                            return Flux.just(content);
                        } catch (Exception e) {
                            return Flux.error(e);
                        }
                    });
                }
            }
        }

        return Flux.error(new TechnicalException(ErrorCode.AI_002, "No provider available"));
    }
}
