package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.TranslationRequest;
import com.ia.aggregator.application.ai.dto.TranslationResult;
import com.ia.aggregator.application.ai.port.in.TranslationUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.TranslationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for text translation.
 *
 * <p>Flow: routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for TRANSLATION capability.
 */
@Service
public class TranslationUseCaseImpl implements TranslationUseCase {

    private static final Capability CAPABILITY = Capability.TRANSLATION;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;

    public TranslationUseCaseImpl(CapabilityRoutingPort router,
                                   AiCapabilityTelemetryPort telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    @Override
    public TranslationResult execute(TranslationRequest request) {
        List<MultiCapabilityProvider> providers = router.resolveAll(CAPABILITY);
        int attempts = 0;
        TechnicalException lastError = null;

        for (MultiCapabilityProvider provider : providers) {
            if (request.model() != null && !provider.supports(request.model())) {
                continue;
            }
            attempts++;
            String modelTag = request.model() == null ? "auto" : request.model();
            telemetry.recordAttempt(CAPABILITY, modelTag, provider.providerName());

            try {
                if (!(provider instanceof TranslationCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                TranslationResult result = capable.translate(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);
                telemetry.recordSuccess(CAPABILITY, modelTag, provider.providerName(), attempts > 1, attempts);
                return result;
            } catch (TechnicalException ex) {
                lastError = ex;
                telemetry.recordFailure(CAPABILITY, modelTag, provider.providerName(), ex.getErrorCode().getCode());
            }
        }

        if (lastError != null) throw lastError;
        throw new TechnicalException(ErrorCode.AI_021, "All translation providers failed");
    }
}
