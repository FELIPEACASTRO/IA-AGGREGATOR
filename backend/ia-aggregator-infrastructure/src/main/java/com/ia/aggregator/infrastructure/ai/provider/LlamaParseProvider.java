package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;
import com.ia.aggregator.application.ai.dto.UsageMetadata;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractDocumentAiProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LlamaParse provider -- document parsing via LlamaIndex Cloud.
 *
 * <p>API: {@code https://api.cloud.llamaindex.ai}
 * <p>Supports: DOCUMENT_PARSING
 * <p>Auth: Bearer token
 * <p>Async: true (upload returns a job ID that must be polled)
 *
 * <p>POST /api/parsing/upload
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.llamaparse.api-key")
public class LlamaParseProvider extends AbstractDocumentAiProvider {

    private static final String PROVIDER_NAME = "llamaparse";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.DOCUMENT_PARSING);

    public LlamaParseProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.llamaparse.api-key:}") String apiKey,
            @Value("${app.ai.providers.llamaparse.base-url:https://api.cloud.llamaindex.ai}") String baseUrl,
            @Value("${app.ai.providers.llamaparse.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.llamaparse.retry-attempts:3}") int retryAttempts,
            @Value("${app.ai.providers.llamaparse.retry-backoff-ms:2000}") long retryBackoffMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderLlamaParse"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public String generate(String prompt, String model) {
        throw new TechnicalException(ErrorCode.AI_007,
                PROVIDER_NAME + " does not support text generation. Use parseDocument() instead.");
    }

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
    protected String parseEndpoint() {
        return "/api/parsing/upload";
    }

    @Override
    protected String buildParseRequest(DocumentParsingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.documentUrl() != null) {
            body.put("url", request.documentUrl());
        } else if (request.documentBase64() != null) {
            body.put("base64", request.documentBase64());
            if (request.fileName() != null) {
                body.put("file_name", request.fileName());
            }
        }
        if (request.outputFormat() != null) {
            body.put("result_type", request.outputFormat());
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build LlamaParse request", e);
        }
    }

    @Override
    protected DocumentParsingResult parseResponse(JsonNode response) {
        // Async response returns job_id for polling; return initial status
        String jobId = response.path("id").asText(response.path("job_id").asText(null));
        String status = response.path("status").asText("pending");

        String content = response.has("text") ? response.path("text").asText("") : "Job submitted: " + jobId;
        int pageCount = response.has("num_pages") ? response.path("num_pages").asInt(0) : 0;

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("job_id", jobId);
        metadata.put("status", status);
        metadata.put("async", true);

        return new DocumentParsingResult(content, pageCount, null, metadata, PROVIDER_NAME, UsageMetadata.UNKNOWN);
    }
}
