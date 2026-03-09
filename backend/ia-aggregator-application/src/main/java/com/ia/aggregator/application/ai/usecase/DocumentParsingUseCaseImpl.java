package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;
import com.ia.aggregator.application.ai.port.in.DocumentParsingUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.DocumentParsingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for structured document parsing.
 *
 * <p>Flow: routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for DOCUMENT_PARSING capability.
 */
@Service
public class DocumentParsingUseCaseImpl implements DocumentParsingUseCase {

    private static final Capability CAPABILITY = Capability.DOCUMENT_PARSING;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;

    public DocumentParsingUseCaseImpl(CapabilityRoutingPort router,
                                       AiCapabilityTelemetryPort telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    @Override
    public DocumentParsingResult execute(DocumentParsingRequest request) {
        List<MultiCapabilityProvider> providers = router.resolveAll(CAPABILITY);
        int attempts = 0;
        TechnicalException lastError = null;

        for (MultiCapabilityProvider provider : providers) {
            attempts++;
            telemetry.recordAttempt(CAPABILITY, "doc", provider.providerName());

            try {
                if (!(provider instanceof DocumentParsingCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                DocumentParsingResult result = capable.parseDocument(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);
                telemetry.recordSuccess(CAPABILITY, "doc", provider.providerName(), attempts > 1, attempts);
                return result;
            } catch (TechnicalException ex) {
                lastError = ex;
                telemetry.recordFailure(CAPABILITY, "doc", provider.providerName(), ex.getErrorCode().getCode());
            }
        }

        if (lastError != null) throw lastError;
        throw new TechnicalException(ErrorCode.AI_022, "All document parsing providers failed");
    }
}
