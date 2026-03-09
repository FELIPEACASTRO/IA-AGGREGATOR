package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationRequest;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationResult;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ThreeDGenerationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * Abstract base for 3D model generation providers.
 * Uses async polling: submit generation task -> poll status -> get result URL.
 *
 * <p>Design Pattern: Template Method — subclasses override:
 * <ul>
 *   <li>{@link #submitEndpoint()} — URL to submit 3D generation job</li>
 *   <li>{@link #buildSubmitBody(ThreeDGenerationRequest)} — build provider-specific request body</li>
 *   <li>{@link #parseJobId(JsonNode)} — extract job ID from submit response</li>
 *   <li>{@link #pollEndpoint(String)} — URL to poll job status</li>
 *   <li>{@link #isJobComplete(JsonNode)} — check if job has finished</li>
 *   <li>{@link #isJobFailed(JsonNode)} — check if job has failed</li>
 *   <li>{@link #parseResult(JsonNode)} — extract final 3D model result</li>
 * </ul>
 *
 * <p>Big O: O(P) where P = number of poll iterations. Bounded by maxPollAttempts.
 */
public abstract class AbstractThreeDProvider implements MultiCapabilityProvider, ThreeDGenerationCapable {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int maxPollAttempts;
    protected final long pollIntervalMs;

    protected AbstractThreeDProvider(
            ObjectMapper objectMapper,
            CircuitBreaker circuitBreaker,
            AuthStrategy authStrategy,
            String baseUrl,
            long timeoutMs,
            int maxPollAttempts,
            long pollIntervalMs
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.authStrategy = authStrategy;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.maxPollAttempts = maxPollAttempts;
        this.pollIntervalMs = pollIntervalMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public boolean supports(String model) {
        return false; // 3D providers typically don't use model selection
    }

    @Override
    public String generate(String prompt, String model) {
        throw new TechnicalException(ErrorCode.AI_023,
                providerName() + " does not support text generation via generate(). Use generate3D().");
    }

    protected abstract String submitEndpoint();
    protected abstract String buildSubmitBody(ThreeDGenerationRequest request);
    protected abstract String parseJobId(JsonNode submitResponse);
    protected abstract String pollEndpoint(String jobId);
    protected abstract boolean isJobComplete(JsonNode pollResponse);
    protected abstract boolean isJobFailed(JsonNode pollResponse);
    protected abstract ThreeDGenerationResult parseResult(JsonNode completedResponse);

    @Override
    public ThreeDGenerationResult generate3D(ThreeDGenerationRequest request) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                // Step 1: Submit job
                String body = buildSubmitBody(request);
                HttpRequest.Builder submitReq = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + submitEndpoint()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofMillis(timeoutMs));
                authStrategy.apply(submitReq);

                HttpResponse<String> submitResp = httpClient.send(submitReq.build(), HttpResponse.BodyHandlers.ofString());
                if (submitResp.statusCode() >= 400) {
                    throw new TechnicalException(ErrorCode.AI_023,
                            providerName() + " submit failed HTTP " + submitResp.statusCode());
                }

                JsonNode submitJson = objectMapper.readTree(submitResp.body());
                String jobId = parseJobId(submitJson);

                // Step 2: Poll until complete
                for (int i = 0; i < maxPollAttempts; i++) {
                    Thread.sleep(pollIntervalMs);

                    HttpRequest.Builder pollReq = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + pollEndpoint(jobId)))
                            .GET()
                            .timeout(Duration.ofMillis(timeoutMs));
                    authStrategy.apply(pollReq);

                    HttpResponse<String> pollResp = httpClient.send(pollReq.build(), HttpResponse.BodyHandlers.ofString());
                    JsonNode pollJson = objectMapper.readTree(pollResp.body());

                    if (isJobFailed(pollJson)) {
                        throw new TechnicalException(ErrorCode.AI_023,
                                providerName() + " 3D generation job failed: " + jobId);
                    }
                    if (isJobComplete(pollJson)) {
                        return parseResult(pollJson);
                    }
                }

                throw new TechnicalException(ErrorCode.AI_015,
                        providerName() + " 3D generation polling timeout after " + maxPollAttempts + " attempts");
            } catch (TechnicalException e) {
                throw e;
            } catch (Exception e) {
                throw new TechnicalException(ErrorCode.AI_023,
                        providerName() + " 3D generation failed: " + e.getMessage());
            }
        });
    }

    @Override
    public Set<com.ia.aggregator.domain.ai.Capability> capabilities() {
        return Set.of(com.ia.aggregator.domain.ai.Capability.THREE_D_GENERATION);
    }
}
