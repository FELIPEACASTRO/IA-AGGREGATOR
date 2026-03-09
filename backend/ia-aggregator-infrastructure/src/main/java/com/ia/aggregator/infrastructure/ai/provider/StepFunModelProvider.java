package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractOpenAiCompatibleProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * StepFun provider — OpenAI-compatible Chinese LLM.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://api.stepfun.com/v1}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.stepfun.api-key")
public class StepFunModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "stepfun";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public StepFunModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.stepfun.api-key:}") String apiKey,
            @Value("${app.ai.providers.stepfun.base-url:https://api.stepfun.com}") String baseUrl,
            @Value("${app.ai.providers.stepfun.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.stepfun.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.stepfun.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.stepfun.supported-models:step-2-16k,step-1-256k,step-1-32k}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderStepfun"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }
}
