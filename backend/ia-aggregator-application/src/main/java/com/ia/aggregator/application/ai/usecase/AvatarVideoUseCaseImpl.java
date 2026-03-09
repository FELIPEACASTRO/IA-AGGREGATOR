package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;
import com.ia.aggregator.application.ai.port.in.AvatarVideoUseCase;
import com.ia.aggregator.application.ai.port.out.AiCapabilityTelemetryPort;
import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.AvatarVideoCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for avatar video generation.
 *
 * <p>Flow: routing → provider iteration with fallback → telemetry.
 * <p>Big O: O(P) where P = providers for AVATAR_VIDEO capability.
 */
@Service
public class AvatarVideoUseCaseImpl implements AvatarVideoUseCase {

    private static final Capability CAPABILITY = Capability.AVATAR_VIDEO;

    private final CapabilityRoutingPort router;
    private final AiCapabilityTelemetryPort telemetry;

    public AvatarVideoUseCaseImpl(CapabilityRoutingPort router,
                                   AiCapabilityTelemetryPort telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    @Override
    public AvatarVideoResult execute(AvatarVideoRequest request) {
        List<MultiCapabilityProvider> providers = router.resolveAll(CAPABILITY);
        int attempts = 0;
        TechnicalException lastError = null;

        for (MultiCapabilityProvider provider : providers) {
            attempts++;
            telemetry.recordAttempt(CAPABILITY, "avatar", provider.providerName());

            try {
                if (!(provider instanceof AvatarVideoCapable capable)) {
                    continue;
                }
                long start = System.currentTimeMillis();
                AvatarVideoResult result = capable.generateAvatarVideo(request);
                telemetry.recordLatency(CAPABILITY, provider.providerName(), System.currentTimeMillis() - start);
                telemetry.recordSuccess(CAPABILITY, "avatar", provider.providerName(), attempts > 1, attempts);
                return result;
            } catch (TechnicalException ex) {
                lastError = ex;
                telemetry.recordFailure(CAPABILITY, "avatar", provider.providerName(), ex.getErrorCode().getCode());
            }
        }

        if (lastError != null) throw lastError;
        throw new TechnicalException(ErrorCode.AI_024, "All avatar video providers failed");
    }
}
