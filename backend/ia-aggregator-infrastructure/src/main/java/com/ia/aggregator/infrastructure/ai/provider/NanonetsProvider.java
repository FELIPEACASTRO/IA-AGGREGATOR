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
 * Nanonets provider -- intelligent document processing and OCR.
 *
 * <p>API: {@code https://app.nanonets.com}
 * <p>Supports: DOCUMENT_PARSING
 * <p>Auth: Bearer token (simplified Basic auth)
 *
 * <p>POST /api/v2/OCR/Model/{modelId}/LabelFile/
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.nanonets.api-key")
public class NanonetsProvider extends AbstractDocumentAiProvider {

    private static final String PROVIDER_NAME = "nanonets";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.DOCUMENT_PARSING);

    private final String modelId;

    public NanonetsProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.nanonets.api-key:}") String apiKey,
            @Value("${app.ai.providers.nanonets.model-id:}") String modelId,
            @Value("${app.ai.providers.nanonets.base-url:https://app.nanonets.com}") String baseUrl,
            @Value("${app.ai.providers.nanonets.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.nanonets.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.nanonets.retry-backoff-ms:2000}") long retryBackoffMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderNanonets"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs);
        this.modelId = modelId;
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
        return "/api/v2/OCR/Model/" + modelId + "/LabelFile/";
    }

    @Override
    protected String buildParseRequest(DocumentParsingRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.documentUrl() != null) {
            body.put("urls", List.of(request.documentUrl()));
        } else if (request.documentBase64() != null) {
            body.put("base64_data", request.documentBase64());
            if (request.fileName() != null) {
                body.put("file_name", request.fileName());
            }
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Nanonets request", e);
        }
    }

    @Override
    protected DocumentParsingResult parseResponse(JsonNode response) {
        StringBuilder content = new StringBuilder();
        List<Map<String, String>> tables = new ArrayList<>();
        int pageCount = 0;

        JsonNode results = response.path("result");
        if (results.isArray()) {
            pageCount = results.size();
            for (JsonNode page : results) {
                JsonNode predictions = page.path("prediction");
                if (predictions.isArray()) {
                    for (JsonNode pred : predictions) {
                        String label = pred.path("label").asText("");
                        String ocrText = pred.path("ocr_text").asText("");

                        content.append(label).append(": ").append(ocrText).append("\n");

                        Map<String, String> row = new LinkedHashMap<>();
                        row.put("label", label);
                        row.put("value", ocrText);
                        row.put("confidence", String.valueOf(pred.path("confidence").asDouble(0)));
                        tables.add(row);
                    }
                }
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model_id", modelId);
        metadata.put("status", response.path("message").asText("completed"));

        return new DocumentParsingResult(
                content.toString().trim(), pageCount, tables, metadata,
                PROVIDER_NAME, UsageMetadata.UNKNOWN);
    }
}
