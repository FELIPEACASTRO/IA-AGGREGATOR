package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeepSeekModelProvider extends AbstractOpenAiCompatibleProvider {

    public DeepSeekModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            AiPricingResolver pricingResolver,
            @Value("${app.ai.providers.deepseek.api-key:}") String apiKey,
            @Value("${app.ai.providers.deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${app.ai.providers.deepseek.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.deepseek.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.deepseek.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.deepseek.supported-models:deepseek-chat,deepseek-reasoner}") List<String> supportedModels
    ) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                "deepseek",
                "DeepSeek",
                "aiProviderDeepseek",
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                List.of("DEEPSEEK_API_KEY")
        );
    }

    DeepSeekModelProvider(
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
