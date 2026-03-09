package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.DocumentParsingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Abstract base for document AI providers (OCR, parsing, extraction).
 *
 * <p>Design Pattern: Template Method — subclasses override:
 * <ul>
 *   <li>{@link #buildParseRequest(DocumentParsingRequest)} — build provider-specific HTTP request body</li>
 *   <li>{@link #parseEndpoint()} — URL to submit document for parsing</li>
 *   <li>{@link #parseResponse(JsonNode)} — extract structured result from provider response</li>
 * </ul>
 *
 * <p>Big O: O(1) per request for synchronous providers.
 * Async providers override {@link #isAsync()} and provide polling logic.
 */
public abstract class AbstractDocumentAiProvider implements MultiCapabilityProvider, DocumentParsingCapable {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;

    protected AbstractDocumentAiProvider(
            ObjectMapper objectMapper,
            CircuitBreaker circuitBreaker,
            AuthStrategy authStrategy,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.authStrategy = authStrategy;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public boolean supports(String model) {
        return false; // Document AI providers typically don't use model selection
    }

    @Override
    public String generate(String prompt, String model) {
        throw new TechnicalException(ErrorCode.AI_022,
                providerName() + " does not support text generation via generate(). Use parseDocument().");
    }

    /** Returns the endpoint URL for document parsing. */
    protected abstract String parseEndpoint();

    /** Builds the JSON request body for the provider. */
    protected abstract String buildParseRequest(DocumentParsingRequest request);

    /** Parses the provider response into a normalized result. */
    protected abstract DocumentParsingResult parseResponse(JsonNode response);

    /** Whether this provider uses async polling. Default: false (synchronous). */
    protected boolean isAsync() {
        return false;
    }

    @Override
    public DocumentParsingResult parseDocument(DocumentParsingRequest request) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                String jsonBody = buildParseRequest(request);
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + parseEndpoint()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofMillis(timeoutMs));

                authStrategy.apply(reqBuilder);
                HttpResponse<String> response = sendWithRetry(reqBuilder.build());

                if (response.statusCode() >= 400) {
                    throw new TechnicalException(ErrorCode.AI_022,
                            providerName() + " HTTP " + response.statusCode() + ": " +
                                    response.body().substring(0, Math.min(200, response.body().length())));
                }

                JsonNode json = objectMapper.readTree(response.body());
                return parseResponse(json);
            } catch (TechnicalException e) {
                throw e;
            } catch (Exception e) {
                throw new TechnicalException(ErrorCode.AI_022,
                        providerName() + " document parsing failed: " + e.getMessage());
            }
        });
    }

    protected HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= retryAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 500 || attempt == retryAttempts) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt == retryAttempts) break;
            }
            Thread.sleep(retryBackoffMs * (attempt + 1));
        }
        throw lastException != null ? lastException :
                new TechnicalException(ErrorCode.AI_022, providerName() + " request failed after retries");
    }

    @Override
    public Set<com.ia.aggregator.domain.ai.Capability> capabilities() {
        return Set.of(com.ia.aggregator.domain.ai.Capability.DOCUMENT_PARSING);
    }
}
