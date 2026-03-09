package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.ThreatIntelRequest;
import com.ia.aggregator.application.ai.dto.ThreatIntelResult;
import com.ia.aggregator.application.ai.port.in.ThreatIntelUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.ComplianceCheckPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.PromptGuardrailPort;
import com.ia.aggregator.application.ai.port.out.capability.ThreatIntelCapable;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for threat intelligence search.
 *
 * <p>Includes compliance gate verification before provider invocation.
 * Access is controlled by the {@code app.ai.compliance.dark-web-enabled} feature flag.
 *
 * <p>Flow: compliance gate → guardrail → routing → provider iteration → telemetry.
 * <p>Big O: O(P) where P = providers for THREAT_INTEL_SEARCH capability.
 */
@Service
public class ThreatIntelUseCaseImpl implements ThreatIntelUseCase {

    private static final Capability CAPABILITY = Capability.THREAT_INTEL_SEARCH;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;
    private final PromptGuardrailPort promptGuardrail;
    private final ComplianceCheckPort complianceCheck;

    public ThreatIntelUseCaseImpl(CapabilityRoutingPort router,
                                  AiCapabilityTelemetryPort telemetry,
                                  PromptGuardrailPort promptGuardrail,
                                  ComplianceCheckPort complianceCheck) {
        this.router = router;
        this.telemetry = telemetry;
        this.promptGuardrail = promptGuardrail;
        this.complianceCheck = complianceCheck;
    }

    @Override
    public ThreatIntelResult execute(ThreatIntelRequest request) {
        // Compliance gate — blocks if dark web access is disabled
        complianceCheck.requireDarkWebAccess();

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
                if (!(provider instanceof ThreatIntelCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                ThreatIntelResult result = capable.searchThreatIntel(request);
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
        throw new TechnicalException(ErrorCode.AI_020, "All threat intelligence providers failed");
    }

    private static String resolveModelTag(String model) {
        return model == null || model.isBlank() ? "auto" : model;
    }
}
