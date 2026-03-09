package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.ThreeDGenerationRequest;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationResult;
import com.ia.aggregator.application.ai.port.in.ThreeDGenerationUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ThreeDGenerationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for 3D model generation.
 *
 * <p>Flow: routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for THREE_D_GENERATION capability.
 */
@Service
public class ThreeDGenerationUseCaseImpl implements ThreeDGenerationUseCase {

    private static final Capability CAPABILITY = Capability.THREE_D_GENERATION;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;

    public ThreeDGenerationUseCaseImpl(CapabilityRoutingPort router,
                                        AiCapabilityTelemetryPort telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    @Override
    public ThreeDGenerationResult execute(ThreeDGenerationRequest request) {
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
                if (!(provider instanceof ThreeDGenerationCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                ThreeDGenerationResult result = capable.generate3D(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);
                telemetry.recordSuccess(CAPABILITY, modelTag, provider.providerName(), attempts > 1, attempts);
                return result;
            } catch (TechnicalException ex) {
                lastError = ex;
                telemetry.recordFailure(CAPABILITY, modelTag, provider.providerName(), ex.getErrorCode().getCode());
            }
        }

        if (lastError != null) throw lastError;
        throw new TechnicalException(ErrorCode.AI_023, "All 3D generation providers failed");
    }
}
