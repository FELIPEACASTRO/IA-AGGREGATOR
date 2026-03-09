package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.VideoGenRequest;
import com.ia.aggregator.application.ai.dto.VideoGenResult;
import com.ia.aggregator.application.ai.port.in.VideoGenerationUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.PromptGuardrailPort;
import com.ia.aggregator.application.ai.port.out.capability.VideoGenerationCapable;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for AI video generation from text prompts.
 *
 * <p>Flow: guardrail → routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for VIDEO_GENERATION capability.
 */
@Service
public class VideoGenerationUseCaseImpl implements VideoGenerationUseCase {

    private static final Capability CAPABILITY = Capability.VIDEO_GENERATION;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;
    private final PromptGuardrailPort promptGuardrail;

    public VideoGenerationUseCaseImpl(CapabilityRoutingPort router,
                                      AiCapabilityTelemetryPort telemetry,
                                      PromptGuardrailPort promptGuardrail) {
        this.router = router;
        this.telemetry = telemetry;
        this.promptGuardrail = promptGuardrail;
    }

    @Override
    public VideoGenResult execute(VideoGenRequest request) {
        try {
            promptGuardrail.validate(request.prompt());
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
                if (!(provider instanceof VideoGenerationCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                VideoGenResult result = capable.generateVideo(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);
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
        throw new TechnicalException(ErrorCode.AI_012, "All video generation providers failed");
    }

    private static String resolveModelTag(String model) {
        return model == null || model.isBlank() ? "auto" : model;
    }
}
