package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.OcrRequest;
import com.ia.aggregator.application.ai.dto.OcrResult;
import com.ia.aggregator.application.ai.port.in.OcrUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.OcrCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for optical character recognition (OCR).
 *
 * <p>Note: No prompt guardrail applied — input is binary image data or URL.
 * <p>Flow: routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for OCR capability.
 */
@Service
public class OcrUseCaseImpl implements OcrUseCase {

    private static final Capability CAPABILITY = Capability.OCR;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;

    public OcrUseCaseImpl(CapabilityRoutingPort router,
                          AiCapabilityTelemetryPort telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    @Override
    public OcrResult execute(OcrRequest request) {
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
                if (!(provider instanceof OcrCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                OcrResult result = capable.ocr(request);
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
        throw new TechnicalException(ErrorCode.AI_017, "All OCR providers failed");
    }

    private static String resolveModelTag(String model) {
        return model == null || model.isBlank() ? "auto" : model;
    }
}
