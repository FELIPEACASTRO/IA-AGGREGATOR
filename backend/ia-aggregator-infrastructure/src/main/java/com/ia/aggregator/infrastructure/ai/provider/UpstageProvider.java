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
 * Upstage provider -- document AI and OCR via Upstage API.
 *
 * <p>API: {@code https://api.upstage.ai}
 * <p>Supports: DOCUMENT_PARSING, OCR
 * <p>Auth: Bearer token
 *
 * <p>POST /v1/document-ai/ocr
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.upstage.api-key")
public class UpstageProvider extends AbstractDocumentAiProvider {

    private static final String PROVIDER_NAME = "upstage";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.DOCUMENT_PARSING, Capability.OCR);

    public UpstageProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.upstage.api-key:}") String apiKey,
            @Value("${app.ai.providers.upstage.base-url:https://api.upstage.ai}") String baseUrl,
            @Value("${app.ai.providers.upstage.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.upstage.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.upstage.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderUpstage"),
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
    protected String parseEndpoint() {
        return "/v1/document-ai/ocr";
    }

    @Override
    protected String buildParseRequest(DocumentParsingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.documentUrl() != null) {
            body.put("document_url", request.documentUrl());
        } else if (request.documentBase64() != null) {
            body.put("document", request.documentBase64());
            if (request.fileName() != null) {
                body.put("file_name", request.fileName());
            }
        }
        if (request.outputFormat() != null) {
            body.put("output_format", request.outputFormat());
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Upstage request", e);
        }
    }

    @Override
    protected DocumentParsingResult parseResponse(JsonNode response) {
        String content = response.path("text").asText(response.path("content").asText(""));
        int pageCount = response.has("pages") && response.path("pages").isArray()
                ? response.path("pages").size()
                : response.path("page_count").asInt(1);

        List<Map<String, String>> tables = new ArrayList<>();
        JsonNode tablesNode = response.path("tables");
        if (tablesNode.isArray()) {
            for (JsonNode table : tablesNode) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("content", table.path("text").asText(null));
                row.put("page", table.path("page").asText(null));
                tables.add(row);
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", response.path("model").asText(null));
        if (response.has("confidence")) {
            metadata.put("confidence", response.path("confidence").asDouble());
        }

        return new DocumentParsingResult(content, pageCount, tables, metadata, PROVIDER_NAME, UsageMetadata.UNKNOWN);
    }
}
