package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.ResponsesRequest;
import com.ia.aggregator.application.ai.dto.ResponsesResult;
import com.ia.aggregator.application.ai.port.in.ResponsesUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.OutputGuardrailPort;
import com.ia.aggregator.application.ai.port.out.PromptGuardrailPort;
import com.ia.aggregator.application.ai.port.out.capability.ResponsesCapable;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Use case for structured responses with tool-use capabilities.
 *
 * <p>Flow: input guardrail → routing → provider iteration → output guardrail → telemetry.
 * <p>Big O: O(P) where P = providers for RESPONSES capability.
 */
@Service
public class ResponsesUseCaseImpl implements ResponsesUseCase {

    private static final Capability CAPABILITY = Capability.RESPONSES;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;
    private final PromptGuardrailPort promptGuardrail;
    private final OutputGuardrailPort outputGuardrail;

    public ResponsesUseCaseImpl(CapabilityRoutingPort router,
                                AiCapabilityTelemetryPort telemetry,
                                PromptGuardrailPort promptGuardrail,
                                OutputGuardrailPort outputGuardrail) {
        this.router = router;
        this.telemetry = telemetry;
        this.promptGuardrail = promptGuardrail;
        this.outputGuardrail = outputGuardrail;
    }

    @Override
    public ResponsesResult execute(ResponsesRequest request) {
        try {
            promptGuardrail.validate(request.input());
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
                if (!(provider instanceof ResponsesCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                ResponsesResult result = capable.responses(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);

                // Output guardrail on text content from structured output
                String textContent = extractTextContent(result.output());
                if (textContent != null && !textContent.isBlank()) {
                    try {
                        outputGuardrail.validate(textContent);
                    } catch (BusinessException ex) {
                        telemetry.recordGuardrailBlocked(CAPABILITY, "output",
                                modelTag, provider.providerName(), ex.getErrorCode().getCode());
                        throw ex;
                    }
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
        throw new TechnicalException(ErrorCode.AI_002, "All responses providers failed");
    }

    /**
     * Extracts text content from structured output items for guardrail validation.
     * Returns null if no text content is found.
     */
    private static String extractTextContent(List<Map<String, Object>> output) {
        if (output == null || output.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : output) {
            Object content = item.get("content");
            if (content instanceof String text) {
                sb.append(text).append(" ");
            } else if (content instanceof List<?> contentList) {
                for (Object contentItem : contentList) {
                    if (contentItem instanceof Map<?, ?> contentMap) {
                        Object text = contentMap.get("text");
                        if (text instanceof String s) {
                            sb.append(s).append(" ");
                        }
                    }
                }
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static String resolveModelTag(String model) {
        return model == null || model.isBlank() ? "auto" : model;
    }
}
