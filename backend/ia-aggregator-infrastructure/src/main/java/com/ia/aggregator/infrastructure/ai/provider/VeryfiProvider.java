package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;
import com.ia.aggregator.application.ai.dto.UsageMetadata;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.auth.CompositeAuthStrategy;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractDocumentAiProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Veryfi provider -- intelligent document parsing for receipts, invoices, and more.
 *
 * <p>API: {@code https://api.veryfi.com/api/v8}
 * <p>Supports: DOCUMENT_PARSING
 * <p>Auth: CLIENT-ID header + AUTHORIZATION: apikey {key}
 *
 * <p>POST /partner/documents
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.veryfi.api-key")
public class VeryfiProvider extends AbstractDocumentAiProvider {

    private static final String PROVIDER_NAME = "veryfi";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.DOCUMENT_PARSING);

    public VeryfiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.veryfi.api-key:}") String apiKey,
            @Value("${app.ai.providers.veryfi.client-id:}") String clientId,
            @Value("${app.ai.providers.veryfi.base-url:https://api.veryfi.com/api/v8}") String baseUrl,
            @Value("${app.ai.providers.veryfi.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.veryfi.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.veryfi.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderVeryfi"),
                new CompositeAuthStrategy(
                        new ApiKeyHeaderAuth("CLIENT-ID", clientId),
                        new ApiKeyHeaderAuth("AUTHORIZATION", "apikey " + apiKey)
                ),
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
        return "/partner/documents";
    }

    @Override
    protected String buildParseRequest(DocumentParsingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.documentUrl() != null) {
            body.put("file_url", request.documentUrl());
        } else if (request.documentBase64() != null) {
            body.put("file_data", request.documentBase64());
            if (request.fileName() != null) {
                body.put("file_name", request.fileName());
            }
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Veryfi request", e);
        }
    }

    @Override
    protected DocumentParsingResult parseResponse(JsonNode response) {
        String content = response.toPrettyString();
        int pageCount = response.has("total_pages") ? response.path("total_pages").asInt(1) : 1;

        List<Map<String, String>> tables = new ArrayList<>();
        JsonNode lineItems = response.path("line_items");
        if (lineItems.isArray()) {
            for (JsonNode item : lineItems) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("description", item.path("description").asText(null));
                row.put("total", item.path("total").asText(null));
                row.put("quantity", item.path("quantity").asText(null));
                tables.add(row);
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vendor_name", response.path("vendor").path("name").asText(null));
        metadata.put("total", response.path("total").asText(null));
        metadata.put("currency", response.path("currency_code").asText(null));

        return new DocumentParsingResult(content, pageCount, tables, metadata, PROVIDER_NAME, UsageMetadata.UNKNOWN);
    }
}
