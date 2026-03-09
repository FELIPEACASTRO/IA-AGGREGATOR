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
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractDocumentAiProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Unstructured provider -- open-source document parsing and ETL.
 *
 * <p>API: {@code https://api.unstructured.io}
 * <p>Supports: DOCUMENT_PARSING
 * <p>Auth: unstructured-api-key header
 *
 * <p>POST /general/v0/general
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.unstructured.api-key")
public class UnstructuredProvider extends AbstractDocumentAiProvider {

    private static final String PROVIDER_NAME = "unstructured";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.DOCUMENT_PARSING);

    public UnstructuredProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.unstructured.api-key:}") String apiKey,
            @Value("${app.ai.providers.unstructured.base-url:https://api.unstructured.io}") String baseUrl,
            @Value("${app.ai.providers.unstructured.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.unstructured.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.unstructured.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderUnstructured"),
                new ApiKeyHeaderAuth("unstructured-api-key", apiKey),
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
        return "/general/v0/general";
    }

    @Override
    protected String buildParseRequest(DocumentParsingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.documentUrl() != null) {
            body.put("url", request.documentUrl());
        } else if (request.documentBase64() != null) {
            body.put("files", request.documentBase64());
            if (request.fileName() != null) {
                body.put("file_filename", request.fileName());
            }
        }
        if (request.outputFormat() != null) {
            body.put("output_format", request.outputFormat());
        }
        body.put("strategy", "auto");
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Unstructured request", e);
        }
    }

    @Override
    protected DocumentParsingResult parseResponse(JsonNode response) {
        StringBuilder content = new StringBuilder();
        List<Map<String, String>> tables = new ArrayList<>();
        int pageCount = 0;

        if (response.isArray()) {
            Set<Integer> pages = new HashSet<>();
            for (JsonNode element : response) {
                String type = element.path("type").asText("");
                String text = element.path("text").asText("");

                if (!text.isBlank()) {
                    content.append(text).append("\n\n");
                }

                if ("Table".equals(type)) {
                    Map<String, String> tableRow = new LinkedHashMap<>();
                    tableRow.put("content", text);
                    tableRow.put("type", type);
                    tables.add(tableRow);
                }

                JsonNode metadata = element.path("metadata");
                if (metadata.has("page_number")) {
                    pages.add(metadata.path("page_number").asInt());
                }
            }
            pageCount = pages.size();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("strategy", "auto");

        return new DocumentParsingResult(
                content.toString().trim(), pageCount, tables, metadata,
                PROVIDER_NAME, UsageMetadata.UNKNOWN);
    }
}
