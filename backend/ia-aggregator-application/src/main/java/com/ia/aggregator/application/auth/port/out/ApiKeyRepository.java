package com.ia.aggregator.application.auth.port.out;

import com.ia.aggregator.application.auth.port.in.ApiKeyUseCase.ApiKeyInfo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for API key persistence.
 */
public interface ApiKeyRepository {

    void save(UUID keyId, UUID userId, String name, String hash, String prefix,
              List<String> scopes, Instant createdAt);

    List<ApiKeyInfo> findByUser(UUID userId);

    Optional<UUID> findUserByHash(String hash);

    void revoke(UUID userId, UUID keyId);

    void recordUsage(UUID keyId);
}
