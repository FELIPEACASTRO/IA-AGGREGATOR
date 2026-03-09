package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.VideoGenerationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Runway provider — Gen-3 Alpha video generation.
 *
 * <p>API: {@code https://api.dev.runwayml.com}
 * <p>Supports: VIDEO_GENERATION
 * <p>Auth: Bearer token + X-Runway-Version header
 *
 * <p>Async: POST /v1/image_to_video -> GET /v1/tasks/{id}
 * <p>Status mapping: "SUCCEEDED" -> completed, "FAILED" -> failed, else processing.
 *
 * <p>Big O: O(P) where P = poll iterations (up to 120 for 600s timeout).
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.runway.api-key")
public class RunwayProvider extends AbstractMediaProvider
        implements VideoGenerationCapable {

    private static final String PROVIDER_NAME = "runway";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.VIDEO_GENERATION);
    private static final String RUNWAY_API_VERSION = "2024-11-06";

    private final String apiKey;

    public RunwayProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.runway.api-key:}") String apiKey,
            @Value("${app.ai.providers.runway.base-url:https://api.dev.runwayml.com}") String baseUrl,
            @Value("${app.ai.providers.runway.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.runway.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.runway.retry-backoff-ms:2000}") long retryBackoffMs,
            @Value("${app.ai.providers.runway.supported-models:gen3a_turbo,gen4_turbo}") List<String> supportedModels,
            @Value("${app.ai.providers.runway.max-poll-attempts:120}") int maxPollAttempts,
            @Value("${app.ai.providers.runway.poll-interval-ms:5000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderRunway"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                supportedModels, maxPollAttempts, pollIntervalMs);
        this.apiKey = apiKey;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    @Override
    public VideoGenResult generateVideo(VideoGenRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("promptText", request.prompt());
        if (request.imageUrl() != null) body.put("promptImage", request.imageUrl());
        if (request.durationSeconds() != null) body.put("duration", request.durationSeconds());
        if (request.aspectRatio() != null) body.put("ratio", request.aspectRatio());

        AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

        return new VideoGenResult(jobStatus.resultUrl(), model, PROVIDER_NAME);
    }

    @Override
    protected String submitJob(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(submitJobEndpoint()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-Runway-Version", RUNWAY_API_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_010,
                        providerName() + " job submission failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return parseSubmitResponse(root);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_010,
                    "Failed to submit job to " + providerName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_010,
                    providerName() + " job submission interrupted", ex);
        }
    }

    @Override
    protected String submitJobEndpoint() {
        return baseUrl + "/v1/image_to_video";
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return responseRoot.path("id").asText();
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/v1/tasks/" + jobId;
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        String status = responseRoot.path("status").asText("PENDING");

        if ("SUCCEEDED".equals(status)) {
            JsonNode output = responseRoot.path("output");
            String resultUrl = output.isArray() && output.size() > 0
                    ? output.get(0).asText()
                    : output.asText(null);
            return new AsyncJobStatus(null, "completed", resultUrl, null, 100, null);
        }

        if ("FAILED".equals(status)) {
            String error = responseRoot.path("failure").asText("Video generation failed");
            return new AsyncJobStatus(null, "failed", null, error, null, null);
        }

        int progress = responseRoot.path("progress").asInt(0);
        return new AsyncJobStatus(null, "processing", null, null, progress, null);
    }
}
