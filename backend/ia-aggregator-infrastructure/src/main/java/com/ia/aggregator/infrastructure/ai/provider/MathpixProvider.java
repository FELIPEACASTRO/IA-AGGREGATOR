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
 * Mathpix provider -- OCR and document parsing specialized for math, science, and tables.
 *
 * <p>API: {@code https://api.mathpix.com}
 * <p>Supports: OCR, DOCUMENT_PARSING
 * <p>Auth: app_key header + app_id header
 *
 * <p>POST /v3/text
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.mathpix.api-key")
public class MathpixProvider extends AbstractDocumentAiProvider {

    private static final String PROVIDER_NAME = "mathpix";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.OCR, Capability.DOCUMENT_PARSING);

    public MathpixProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.mathpix.api-key:}") String apiKey,
            @Value("${app.ai.providers.mathpix.app-id:}") String appId,
            @Value("${app.ai.providers.mathpix.base-url:https://api.mathpix.com}") String baseUrl,
            @Value("${app.ai.providers.mathpix.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.mathpix.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.mathpix.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderMathpix"),
                new CompositeAuthStrategy(
                        new ApiKeyHeaderAuth("app_id", appId),
                        new ApiKeyHeaderAuth("app_key", apiKey)
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
        return "/v3/text";
    }

    @Override
    protected String buildParseRequest(DocumentParsingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.documentUrl() != null) {
            body.put("url", request.documentUrl());
        } else if (request.documentBase64() != null) {
            body.put("src", "data:image/png;base64," + request.documentBase64());
        }
        // Request all supported output formats
        body.put("formats", List.of("text", "latex_styled", "data"));
        body.put("data_options", Map.of("include_table_html", true));
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Mathpix request", e);
        }
    }

    @Override
    protected DocumentParsingResult parseResponse(JsonNode response) {
        String text = response.path("text").asText("");
        String latex = response.path("latex_styled").asText(null);

        String content = latex != null ? latex : text;
        int pageCount = response.has("page_count") ? response.path("page_count").asInt(1) : 1;

        List<Map<String, String>> tables = new ArrayList<>();
        JsonNode dataNode = response.path("data");
        if (dataNode.isArray()) {
            for (JsonNode item : dataNode) {
                if ("table".equals(item.path("type").asText())) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("html", item.path("value").asText(null));
                    tables.add(row);
                }
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (response.has("confidence")) {
            metadata.put("confidence", response.path("confidence").asDouble());
        }
        if (response.has("confidence_rate")) {
            metadata.put("confidence_rate", response.path("confidence_rate").asDouble());
        }
        metadata.put("is_printed", response.path("is_printed").asBoolean(true));

        return new DocumentParsingResult(content, pageCount, tables, metadata, PROVIDER_NAME, UsageMetadata.UNKNOWN);
    }
}
