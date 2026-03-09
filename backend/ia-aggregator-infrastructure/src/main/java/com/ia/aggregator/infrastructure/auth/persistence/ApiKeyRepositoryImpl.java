package com.ia.aggregator.infrastructure.auth.persistence;

import com.ia.aggregator.application.auth.port.in.ApiKeyUseCase.ApiKeyInfo;
import com.ia.aggregator.application.auth.port.out.ApiKeyRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stub (no-op) implementation of ApiKeyRepository.
 * TODO: Replace with real persistence implementation.
 */
@Component
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    @Override
    public void save(UUID keyId, UUID userId, String name, String hash, String prefix,
                     List<String> scopes, Instant createdAt) {
        // no-op
    }

    @Override
    public List<ApiKeyInfo> findByUser(UUID userId) {
        return Collections.emptyList();
    }

    @Override
    public Optional<UUID> findUserByHash(String hash) {
        return Optional.empty();
    }

    @Override
    public void revoke(UUID userId, UUID keyId) {
        // no-op
    }

    @Override
    public void recordUsage(UUID keyId) {
        // no-op
    }
}
