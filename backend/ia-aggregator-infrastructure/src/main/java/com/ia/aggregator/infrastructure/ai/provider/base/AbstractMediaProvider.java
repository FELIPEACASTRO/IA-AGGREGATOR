package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AsyncJobStatus;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract base for media providers (image/video generation).
 * Implements async polling pattern: submit job -> poll status -> get result.
 *
 * <p>Design Pattern: Template Method — subclasses override:
 * <ul>
 *   <li>{@link #submitJobEndpoint()} — URL to submit a new generation job</li>
 *   <li>{@link #parseSubmitResponse(JsonNode)} — extract job ID from submission response</li>
 *   <li>{@link #pollJobEndpoint(String)} — URL to poll job status</li>
 *   <li>{@link #parsePollResponse(JsonNode)} — extract status from poll response</li>
 * </ul>
 *
 * <p>Big O: O(P) where P = number of poll iterations. Bounded by maxPollAttempts.
 */
public abstract class AbstractMediaProvider implements MultiCapabilityProvider {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;
    protected final int maxPollAttempts;
    protected final long pollIntervalMs;

    protected AbstractMediaProvider(
            ObjectMapper objectMapper,
            CircuitBreaker circuitBreaker,
            AuthStrategy authStrategy,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            List<String> supportedModels,
            int maxPollAttempts,
            long pollIntervalMs
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.authStrategy = authStrategy;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.supportedModels = supportedModels;
        this.maxPollAttempts = maxPollAttempts;
        this.pollIntervalMs = pollIntervalMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public boolean supports(String model) {
        return supportedModels.stream().map(String::trim).anyMatch(model::equals);
    }

    @Override
    public String generate(String prompt, String model) {
        throw new TechnicalException(ErrorCode.AI_007,
                providerName() + " does not support text generation via generate(). Use specific media methods.");
    }

    // ---------- Abstract template methods ----------

    /** Returns the endpoint URL for submitting a new generation job. */
    protected abstract String submitJobEndpoint();

    /** Parses the job ID from the submission response. */
    protected abstract String parseSubmitResponse(JsonNode responseRoot);

    /** Returns the endpoint URL for polling job status. */
    protected abstract String pollJobEndpoint(String jobId);

    /** Parses the poll response into an AsyncJobStatus. */
    protected abstract AsyncJobStatus parsePollResponse(JsonNode responseRoot);

    // ---------- Core async execution ----------

    /**
     * Submits a job and polls until completion or timeout.
     *
     * @param payload the JSON-serializable request body
     * @return the completed AsyncJobStatus with result URL
     * @throws TechnicalException if submission fails, polling times out, or job fails
     */
    protected AsyncJobStatus submitAndPoll(Object payload) {
        String jobId = submitJob(payload);
        return pollUntilComplete(jobId);
    }

    protected String submitJob(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(submitJobEndpoint()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(requestBuilder);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

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

    protected AsyncJobStatus pollUntilComplete(String jobId) {
        for (int i = 0; i < maxPollAttempts; i++) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new TechnicalException(ErrorCode.AI_015,
                        providerName() + " polling interrupted", ex);
            }

            AsyncJobStatus status = pollJob(jobId);

            if (status.isCompleted()) {
                return status;
            }
            if (status.isFailed()) {
                throw new TechnicalException(ErrorCode.AI_010,
                        providerName() + " job failed: " + status.errorMessage());
            }
        }

        throw new TechnicalException(ErrorCode.AI_015,
                providerName() + " job polling timeout after " + maxPollAttempts + " attempts");
    }

    private AsyncJobStatus pollJob(String jobId) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(pollJobEndpoint(jobId)))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET();

            authStrategy.apply(requestBuilder);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_015,
                        providerName() + " poll request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return parsePollResponse(root);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_015,
                    "Failed to poll " + providerName() + " job", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_015,
                    providerName() + " poll interrupted", ex);
        }
    }

    protected <T> T executeWithRetry(Supplier<T> call) {
        TechnicalException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (TechnicalException ex) {
                lastError = ex;
                if (attempt == maxAttempts) throw ex;
                sleepBackoff();
            }
        }
        throw lastError;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) return;
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_010,
                    providerName() + " retry interrupted", ex);
        }
    }
}
