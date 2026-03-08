package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class XaiModelProvider extends AbstractOpenAiCompatibleProvider {

    public XaiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            AiPricingResolver pricingResolver,
            @Value("${app.ai.providers.xai.api-key:}") String apiKey,
            @Value("${app.ai.providers.xai.base-url:https://api.x.ai/v1}") String baseUrl,
            @Value("${app.ai.providers.xai.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.xai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.xai.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.xai.supported-models:grok-2-latest,grok-3-latest}") List<String> supportedModels
    ) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                "xai",
                "xAI",
                "aiProviderXai",
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                List.of("XAI_API_KEY")
        );
    }

    XaiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            String apiKey,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            List<String> supportedModels
    ) {
        this(objectMapper, circuitBreakerRegistry, AiPricingResolver.noop(), apiKey, baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
    }
}
