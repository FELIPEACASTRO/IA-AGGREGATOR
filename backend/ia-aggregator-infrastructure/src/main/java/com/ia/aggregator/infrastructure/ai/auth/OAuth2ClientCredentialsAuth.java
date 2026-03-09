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
 * OAuth2 Client Credentials authentication strategy.
 * Obtains and caches an access token from a token endpoint.
 * Used by: Google Cloud services (Vision, Speech, NLP, Translation, TTS).
 *
 * <p>Thread-safe: uses ReentrantLock for token refresh. O(1) for cached token lookup.
 */
public class OAuth2ClientCredentialsAuth implements AuthStrategy {

    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public OAuth2ClientCredentialsAuth(String tokenEndpoint, String clientId,
                                       String clientSecret, String scope) {
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
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
            String body = "grant_type=client_credentials"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + (scope != null ? "&scope=" + scope : "");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OAuth2 token request failed with status " + response.statusCode());
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
            throw new RuntimeException("Failed to obtain OAuth2 access token", e);
        }
    }
}
