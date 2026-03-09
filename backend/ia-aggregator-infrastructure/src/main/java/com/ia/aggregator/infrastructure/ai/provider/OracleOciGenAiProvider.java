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
 * Oracle OCI Generative AI provider -- OpenAI-compatible API for OCI-hosted models.
 *
 * <p>API: {@code https://inference.generativeai.{region}.oci.oraclecloud.com}
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Auth: Bearer token (OCI managed service token)
 *
 * <p>Uses OpenAI-compatible format via AbstractOpenAiCompatibleProvider.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.oracle-oci-genai.api-key")
public class OracleOciGenAiProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "oracle-oci-genai";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    private final String compartmentId;

    public OracleOciGenAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.oracle-oci-genai.api-key:}") String apiKey,
            @Value("${app.ai.providers.oracle-oci-genai.region:us-chicago-1}") String region,
            @Value("${app.ai.providers.oracle-oci-genai.compartment-id:}") String compartmentId,
            @Value("${app.ai.providers.oracle-oci-genai.base-url:}") String baseUrlOverride,
            @Value("${app.ai.providers.oracle-oci-genai.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.oracle-oci-genai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.oracle-oci-genai.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.oracle-oci-genai.supported-models:cohere.command-r-plus,meta.llama-3.1-405b-instruct}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderOracleOciGenAi"),
                new BearerTokenAuth(apiKey),
                baseUrlOverride != null && !baseUrlOverride.isBlank()
                        ? baseUrlOverride
                        : "https://inference.generativeai." + region + ".oci.oraclecloud.com",
                timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.compartmentId = compartmentId;
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    protected String chatEndpointPath() {
        return "/v1/chat/completions";
    }

    @Override
    protected String embeddingEndpointPath() {
        return "/v1/embeddings";
    }

    @Override
    protected String resolveDefaultEmbeddingModel() {
        return "cohere.embed-english-v3.0";
    }
}
