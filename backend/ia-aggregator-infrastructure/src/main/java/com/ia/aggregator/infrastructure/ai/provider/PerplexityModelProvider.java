package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PerplexityModelProvider extends AbstractOpenAiCompatibleProvider {

    public PerplexityModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            AiPricingResolver pricingResolver,
            @Value("${app.ai.providers.perplexity.api-key:}") String apiKey,
            @Value("${app.ai.providers.perplexity.base-url:https://api.perplexity.ai}") String baseUrl,
            @Value("${app.ai.providers.perplexity.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.perplexity.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.perplexity.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.perplexity.supported-models:sonar,sonar-pro}") List<String> supportedModels
    ) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                "perplexity",
                "Perplexity",
                "aiProviderPerplexity",
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                List.of("PERPLEXITY_API_KEY")
        );
    }

    PerplexityModelProvider(
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
