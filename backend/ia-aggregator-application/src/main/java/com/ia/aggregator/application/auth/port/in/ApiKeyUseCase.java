package com.ia.aggregator.application.auth.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Use case for API key lifecycle management.
 */
public interface ApiKeyUseCase {

    /**
     * Create a new API key for a user.
     *
     * @param userId the user ID
     * @param name   human-readable key name
     * @param scopes permissions for this key
     * @return the API key details (key is only visible once at creation time)
     */
    ApiKeyResult create(UUID userId, String name, List<String> scopes);

    /**
     * List all API keys for a user (keys are masked).
     */
    List<ApiKeyInfo> listByUser(UUID userId);

    /**
     * Revoke an API key.
     */
    void revoke(UUID userId, UUID keyId);

    /**
     * Validate an API key and return the associated user ID.
     *
     * @param rawKey the raw API key from the request header
     * @return the user ID if valid
     */
    UUID validate(String rawKey);

    record ApiKeyResult(UUID keyId, String name, String rawKey, String prefix, List<String> scopes) {}
    record ApiKeyInfo(UUID keyId, String name, String prefix, List<String> scopes,
                      java.time.Instant createdAt, java.time.Instant lastUsedAt) {}
}
