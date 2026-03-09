package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.GroundedChatRequest;
import com.ia.aggregator.application.ai.dto.GroundedChatResult;
import com.ia.aggregator.application.ai.port.in.GroundedChatUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.OutputGuardrailPort;
import com.ia.aggregator.application.ai.port.out.PromptGuardrailPort;
import com.ia.aggregator.application.ai.port.out.capability.WebGroundedChatCapable;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for web-grounded chat — answers backed by real-time web search.
 *
 * <p>Flow: input guardrail → routing → provider iteration → output guardrail → telemetry.
 * <p>Big O: O(P) where P = providers for WEB_GROUNDED_CHAT capability.
 */
@Service
public class GroundedChatUseCaseImpl implements GroundedChatUseCase {

    private static final Capability CAPABILITY = Capability.WEB_GROUNDED_CHAT;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;
    private final PromptGuardrailPort promptGuardrail;
    private final OutputGuardrailPort outputGuardrail;

    public GroundedChatUseCaseImpl(CapabilityRoutingPort router,
                                   AiCapabilityTelemetryPort telemetry,
                                   PromptGuardrailPort promptGuardrail,
                                   OutputGuardrailPort outputGuardrail) {
        this.router = router;
        this.telemetry = telemetry;
        this.promptGuardrail = promptGuardrail;
        this.outputGuardrail = outputGuardrail;
    }

    @Override
    public GroundedChatResult execute(GroundedChatRequest request) {
        try {
            promptGuardrail.validate(request.query());
        } catch (BusinessException ex) {
            telemetry.recordGuardrailBlocked(CAPABILITY, "prompt",
                    resolveModelTag(request.model()), "pre_provider", ex.getErrorCode().getCode());
            throw ex;
        }

        List<MultiCapabilityProvider> providers = router.resolveAll(CAPABILITY);
        int attempts = 0;
        TechnicalException lastError = null;

        for (MultiCapabilityProvider provider : providers) {
            if (request.model() != null && !provider.supports(request.model())) {
                continue;
            }

            attempts++;
            String modelTag = resolveModelTag(request.model());
            telemetry.recordAttempt(CAPABILITY, modelTag, provider.providerName());

            try {
                if (!(provider instanceof WebGroundedChatCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                GroundedChatResult result = capable.groundedChat(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);

                // Output guardrail on generated text
                try {
                    outputGuardrail.validate(result.content());
                } catch (BusinessException ex) {
                    telemetry.recordGuardrailBlocked(CAPABILITY, "output",
                            modelTag, provider.providerName(), ex.getErrorCode().getCode());
                    throw ex;
                }

                telemetry.recordSuccess(CAPABILITY, modelTag, provider.providerName(), attempts > 1, attempts);
                return result;
            } catch (TechnicalException ex) {
                lastError = ex;
                telemetry.recordFailure(CAPABILITY, modelTag, provider.providerName(), ex.getErrorCode().getCode());
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new TechnicalException(ErrorCode.AI_018, "All grounded chat providers failed");
    }

    private static String resolveModelTag(String model) {
        return model == null || model.isBlank() ? "auto" : model;
    }
}
