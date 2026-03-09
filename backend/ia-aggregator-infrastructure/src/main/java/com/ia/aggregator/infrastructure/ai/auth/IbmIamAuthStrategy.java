package com.ia.aggregator.infrastructure.ai.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * IBM IAM authentication strategy for Watson and IBM Cloud services.
 *
 * <p>Exchanges an IBM Cloud API key for a short-lived IAM bearer token via:
 * {@code POST https://iam.cloud.ibm.com/identity/token}
 * with {@code grant_type=urn:ibm:params:oauth:grant-type:apikey&apikey={key}}
 *
 * <p>Thread-safe: uses ReentrantLock for token refresh with double-checked locking.
 * O(1) for cached token lookup.
 *
 * <p>Similar to {@link OAuth2ClientCredentialsAuth} but uses IBM-specific grant type
 * and token endpoint format.
 */
public class IbmIamAuthStrategy implements AuthStrategy {

    private static final String IBM_IAM_TOKEN_ENDPOINT = "https://iam.cloud.ibm.com/identity/token";
    private static final String IBM_GRANT_TYPE = "urn:ibm:params:oauth:grant-type:apikey";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public IbmIamAuthStrategy(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        return builder.header("Authorization", "Bearer " + getAccessToken());
    }

    private String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
                return cachedToken;
            }

            return refreshToken();
        } finally {
            tokenLock.unlock();
        }
    }

    private String refreshToken() {
        try {
            String body = "grant_type=" + IBM_GRANT_TYPE + "&apikey=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(IBM_IAM_TOKEN_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("IBM IAM token request failed with status " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
            // Refresh 60 seconds before expiry to avoid edge cases
            tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);

            return cachedToken;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain IBM IAM access token", e);
        }
    }
}
