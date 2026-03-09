package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.SpeechToTextRequest;
import com.ia.aggregator.application.ai.dto.TranscriptionResult;
import com.ia.aggregator.application.ai.port.in.SpeechToTextUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for speech-to-text transcription.
 *
 * <p>Note: No prompt guardrail applied — input is binary audio data.
 * <p>Flow: routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for SPEECH_TO_TEXT capability.
 */
@Service
public class SpeechToTextUseCaseImpl implements SpeechToTextUseCase {

    private static final Capability CAPABILITY = Capability.SPEECH_TO_TEXT;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;

    public SpeechToTextUseCaseImpl(CapabilityRoutingPort router,
                                   AiCapabilityTelemetryPort telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    @Override
    public TranscriptionResult execute(SpeechToTextRequest request) {
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
                if (!(provider instanceof SpeechToTextCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                TranscriptionResult result = capable.transcribe(request);
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
        throw new TechnicalException(ErrorCode.AI_013, "All speech-to-text providers failed");
    }

    private static String resolveModelTag(String model) {
        return model == null || model.isBlank() ? "auto" : model;
    }
}
