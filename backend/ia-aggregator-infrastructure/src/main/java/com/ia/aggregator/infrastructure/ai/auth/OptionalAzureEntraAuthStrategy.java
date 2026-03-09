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
 * Optional Azure Entra ID (formerly Azure Active Directory) authentication strategy.
 *
 * <p>Enterprise alternative to API key auth for Azure OpenAI. Uses OAuth2 client
 * credentials flow against the Azure AD token endpoint:
 * <pre>
 *   POST https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token
 *   grant_type=client_credentials
 *   &client_id={clientId}
 *   &client_secret={clientSecret}
 *   &scope=https://cognitiveservices.azure.com/.default
 * </pre>
 *
 * <p>The obtained token is sent as {@code Authorization: Bearer {token}}.
 * Tokens are cached and refreshed 60 seconds before expiry (thread-safe double-check lock).
 *
 * <p>Usage: instantiate only when {@code azure.use-entra-auth=true} is configured.
 * When disabled, use {@link AzureApiKeyAuthStrategy} instead.
 *
 * <p>Big O: O(1) for cached token lookup; O(1) for token refresh (single HTTP call).
 * Thread safety: ReentrantLock with double-checked locking for token refresh.
 */
public class OptionalAzureEntraAuthStrategy implements AuthStrategy {

    private static final String AZURE_AD_TOKEN_URL =
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String COGNITIVE_SERVICES_SCOPE =
            "https://cognitiveservices.azure.com/.default";

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    /**
     * @param tenantId     Azure AD tenant ID (from Azure Portal → App Registrations)
     * @param clientId     Service principal client/application ID
     * @param clientSecret Service principal client secret
     */
    public OptionalAzureEntraAuthStrategy(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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
            String tokenUrl = String.format(AZURE_AD_TOKEN_URL, tenantId);
            String body = "grant_type=client_credentials"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&scope=" + COGNITIVE_SERVICES_SCOPE;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Azure Entra token request failed with status " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
            tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);

            return cachedToken;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Azure Entra access token for tenant=" + tenantId, e);
        }
    }
}
