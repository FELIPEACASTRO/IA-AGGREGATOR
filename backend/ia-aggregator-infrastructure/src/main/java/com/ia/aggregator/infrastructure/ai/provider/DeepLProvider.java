package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.TranslationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * DeepL provider -- machine translation via DeepL API.
 *
 * <p>API: {@code https://api.deepl.com} (Pro) or {@code https://api-free.deepl.com} (Free)
 * <p>Supports: TRANSLATION
 * <p>Auth: DeepL-Auth-Key header
 *
 * <p>POST /v2/translate with form-encoded: text, target_lang, source_lang
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.deepl.api-key")
public class DeepLProvider implements MultiCapabilityProvider, TranslationCapable {

    private static final String PROVIDER_NAME = "deepl";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TRANSLATION);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public DeepLProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.deepl.api-key:}") String apiKey,
            @Value("${app.ai.providers.deepl.base-url:https://api.deepl.com}") String baseUrl,
            @Value("${app.ai.providers.deepl.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.deepl.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.deepl.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.deepl.supported-models:deepl-pro}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderDeepL");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.supportedModels = supportedModels;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public boolean supports(String model) {
        return apiKey != null
                && !apiKey.isBlank()
                && supportedModels.stream().map(String::trim).anyMatch(model::equals);
    }

    @Override
    public String generate(String prompt, String model) {
        throw new TechnicalException(ErrorCode.AI_007,
                PROVIDER_NAME + " does not support text generation. Use translate() instead.");
    }

    @Override
    public TranslationResult translate(TranslationRequest request) {
        Supplier<TranslationResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callTranslate(request));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "DeepL circuit breaker is open", ex);
        }
    }

    private TranslationResult callTranslate(TranslationRequest request) {
        try {
            StringBuilder formBody = new StringBuilder();
            formBody.append("text=").append(URLEncoder.encode(request.text(), StandardCharsets.UTF_8));
            formBody.append("&target_lang=").append(URLEncoder.encode(request.targetLang(), StandardCharsets.UTF_8));
            if (request.sourceLang() != null) {
                formBody.append("&source_lang=").append(URLEncoder.encode(request.sourceLang(), StandardCharsets.UTF_8));
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/translate"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "DeepL-Auth-Key " + apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode translations = root.path("translations");

            if (!translations.isArray() || translations.isEmpty()) {
                throw new TechnicalException(ErrorCode.AI_005, "DeepL returned empty translations");
            }

            JsonNode first = translations.get(0);
            String translatedText = first.path("text").asText(null);
            String detectedSourceLang = first.path("detected_source_language").asText(null);

            return new TranslationResult(translatedText, detectedSourceLang, PROVIDER_NAME, UsageMetadata.UNKNOWN);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse DeepL response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "DeepL request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "DeepL translation request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "DeepL rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "DeepL provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "DeepL rejected request: " + response.statusCode());
    }

    private <T> T executeWithRetry(Supplier<T> guardedCall) {
        TechnicalException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return guardedCall.get();
            } catch (TechnicalException ex) {
                lastError = ex;
                if (!shouldRetry(ex) || attempt == maxAttempts) throw ex;
                sleepBackoff();
            }
        }
        throw lastError == null
                ? new TechnicalException(ErrorCode.AI_002, "DeepL request failed after retries")
                : lastError;
    }

    private boolean shouldRetry(TechnicalException ex) {
        return ex.getErrorCode() == ErrorCode.AI_002
                || ex.getErrorCode() == ErrorCode.AI_003
                || ex.getErrorCode() == ErrorCode.AI_005;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) return;
        try { Thread.sleep(retryBackoffMs); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "DeepL retry interrupted", e);
        }
    }
}
