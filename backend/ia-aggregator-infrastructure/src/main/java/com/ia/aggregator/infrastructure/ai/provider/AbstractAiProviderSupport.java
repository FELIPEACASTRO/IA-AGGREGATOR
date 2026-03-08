package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AiCostEstimate;
import com.ia.aggregator.application.ai.dto.AiHealthStatus;
import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import com.ia.aggregator.application.ai.dto.AiUsageEstimate;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import com.ia.aggregator.common.exception.AiGatewayException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.infrastructure.security.SensitiveDataSanitizer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

abstract class AbstractAiProviderSupport implements AiModelProvider {

    private static final Random JITTER = new Random();

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AiPricingResolver pricingResolver;
    protected final String providerId;
    protected final String displayName;
    protected final String apiKey;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;
    protected final List<String> requiredSecrets;
    protected final boolean streamingSupported;

    protected AbstractAiProviderSupport(ObjectMapper objectMapper,
                                        CircuitBreakerRegistry circuitBreakerRegistry,
                                        AiPricingResolver pricingResolver,
                                        String providerId,
                                        String displayName,
                                        String circuitBreakerName,
                                        String apiKey,
                                        String baseUrl,
                                        long timeoutMs,
                                        int retryAttempts,
                                        long retryBackoffMs,
                                        List<String> supportedModels,
                                        List<String> requiredSecrets,
                                        boolean streamingSupported) {
        this.objectMapper = objectMapper;
        this.pricingResolver = pricingResolver;
        this.providerId = providerId;
        this.displayName = displayName;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.supportedModels = supportedModels == null ? List.of() : supportedModels.stream().map(String::trim).filter(item -> !item.isBlank()).toList();
        this.requiredSecrets = requiredSecrets;
        this.streamingSupported = streamingSupported;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(timeoutMs, 30_000)))
                .build();
    }

    @Override
    public String providerName() {
        return displayName;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public boolean supports(String model) {
        return isConfigured() && supportedModels.stream().anyMatch(model::equals);
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public List<String> requiredSecrets() {
        return requiredSecrets;
    }

    @Override
    public List<String> supportedModels() {
        return supportedModels;
    }

    @Override
    public boolean supportsStreaming() {
        return streamingSupported;
    }

    @Override
    public String defaultModel() {
        return supportedModels.isEmpty() ? null : supportedModels.getFirst();
    }

    @Override
    public String generate(String prompt, String model) {
        return sendPrompt(new AiPromptRequest(null, prompt, providerId(), model)).content();
    }

    @Override
    public AiHealthStatus healthCheck() {
        return new AiHealthStatus(
                providerId(),
                providerName(),
                isConfigured(),
                true,
                isConfigured(),
                circuitBreaker.getState().name(),
                Instant.now(),
                null,
                null,
                supportsStreaming(),
                defaultModel(),
                supportedModels(),
                requiredSecrets()
        );
    }

    @Override
    public AiCostEstimate estimateCost(AiPromptRequest request, AiUsageEstimate usage) {
        return pricingResolver.resolve(providerId(), resolveModel(request), usage);
    }

    protected String resolveModel(AiPromptRequest request) {
        String requestedModel = request.model();
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel.trim();
        }
        return defaultModel();
    }

    protected Duration resolveTimeout(boolean stream) {
        long lowerBound = stream ? 90_000L : 30_000L;
        return Duration.ofMillis(Math.max(timeoutMs, lowerBound));
    }

    protected AiPromptResponse executePrompt(AiPromptRequest request, Supplier<AiPromptResponse> supplier) {
        if (!isConfigured()) {
            throw new AiGatewayException(
                    ErrorCode.AI_008,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    false,
                    "Provider " + providerName() + " nao configurado. Defina " + String.join(", ", requiredSecrets())
            );
        }

        if (!supportsModel(resolveModel(request))) {
            throw new AiGatewayException(
                    ErrorCode.AI_007,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    false,
                    "Modelo nao suportado pelo provider " + providerName()
            );
        }

        Supplier<AiPromptResponse> guardedCall = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        try {
            return executeWithRetry(guardedCall, request);
        } catch (CallNotPermittedException exception) {
            throw new AiGatewayException(
                    ErrorCode.AI_009,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    true,
                    "Circuit breaker aberto para o provider " + providerName(),
                    exception
            );
        }
    }

    private AiPromptResponse executeWithRetry(Supplier<AiPromptResponse> guardedCall, AiPromptRequest request) {
        AiGatewayException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return guardedCall.get();
            } catch (AiGatewayException exception) {
                lastError = exception;
                if (!exception.isRetryable() || attempt == maxAttempts) {
                    throw exception;
                }
                sleepBackoff(attempt, request.requestId());
            }
        }

        if (lastError != null) {
            throw lastError;
        }

        throw new AiGatewayException(
                ErrorCode.AI_002,
                request.requestId(),
                providerId(),
                resolveModel(request),
                true,
                "Falha ao executar o provider " + providerName()
        );
    }

    protected void sleepBackoff(int attempt, String requestId) {
        if (retryBackoffMs <= 0) {
            return;
        }

        long exponential = retryBackoffMs * Math.max(1L, (long) Math.pow(2, Math.max(0, attempt - 1)));
        long jitter = (long) (exponential * (0.15d + (JITTER.nextDouble() * 0.20d)));
        long totalSleep = exponential + jitter;

        try {
            Thread.sleep(totalSleep);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiGatewayException(
                    ErrorCode.AI_002,
                    requestId,
                    providerId(),
                    null,
                    true,
                    "Retry interrompido para o provider " + providerName(),
                    exception
            );
        }
    }

    protected JsonNode readJson(String payload, AiPromptRequest request, String parseContext) {
        try {
            return objectMapper.readTree(payload);
        } catch (IOException exception) {
            throw new AiGatewayException(
                    ErrorCode.AI_005,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    false,
                    "Falha ao interpretar a resposta do provider " + providerName() + " em " + parseContext,
                    exception
            );
        }
    }

    protected HttpResponse<String> send(HttpRequest request, AiPromptRequest promptRequest) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiGatewayException(
                    ErrorCode.AI_002,
                    promptRequest.requestId(),
                    providerId(),
                    resolveModel(promptRequest),
                    true,
                    "Requisicao interrompida para o provider " + providerName(),
                    exception
            );
        } catch (IOException exception) {
            throw new AiGatewayException(
                    ErrorCode.AI_002,
                    promptRequest.requestId(),
                    providerId(),
                    resolveModel(promptRequest),
                    true,
                    "Falha de rede ao acessar o provider " + providerName(),
                    exception
            );
        }
    }

    protected HttpRequest.Builder baseJsonRequest(String path, AiPromptRequest request) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(resolveTimeout(request.stream()))
                .header("Content-Type", "application/json");
    }

    protected AiGatewayException mapHttpError(AiPromptRequest request, int statusCode, String responseBody) {
        String safeBody = SensitiveDataSanitizer.sanitize(responseBody);
        if (statusCode == 408 || statusCode == 429) {
            return new AiGatewayException(
                    ErrorCode.AI_003,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    true,
                    "Limite ou timeout atingido no provider " + providerName() + " (status " + statusCode + ")"
            );
        }
        if (statusCode == 401 || statusCode == 403) {
            return new AiGatewayException(
                    ErrorCode.AI_008,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    false,
                    "Credencial invalida ou sem permissao para o provider " + providerName()
            );
        }
        if (statusCode >= 500) {
            return new AiGatewayException(
                    ErrorCode.AI_002,
                    request.requestId(),
                    providerId(),
                    resolveModel(request),
                    true,
                    "Provider " + providerName() + " indisponivel (status " + statusCode + ")"
            );
        }
        return new AiGatewayException(
                ErrorCode.AI_007,
                request.requestId(),
                providerId(),
                resolveModel(request),
                false,
                "Requisicao rejeitada pelo provider " + providerName() + " (status " + statusCode + "): " + safeBody
        );
    }

    protected AiPromptResponse buildResponse(AiPromptRequest request,
                                             String content,
                                             AiUsageEstimate usage,
                                             long startedAtMs,
                                             String finishReason) {
        String model = resolveModel(request);
        return new AiPromptResponse(
                content,
                providerName(),
                model,
                request.requestId(),
                usage == null ? AiUsageEstimate.empty() : usage,
                estimateCost(request, usage == null ? AiUsageEstimate.empty() : usage),
                Math.max(0L, System.currentTimeMillis() - startedAtMs),
                finishReason == null || finishReason.isBlank() ? "completed" : finishReason,
                List.of()
        );
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
