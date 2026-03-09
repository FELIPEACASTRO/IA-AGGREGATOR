package com.ia.aggregator.application.platform.port.in;

import com.ia.aggregator.domain.platform.VirtualKey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case for virtual API key management.
 */
public interface VirtualKeyUseCase {

    VirtualKeyResult create(UUID orgId, UUID userId, String name,
                             List<String> allowedModels, List<String> allowedCapabilities,
                             int rateLimitRpm, double budgetLimitUsd, Instant expiresAt);

    VirtualKey getById(UUID keyId);

    List<VirtualKey> listByOrg(UUID orgId);

    VirtualKey validate(String rawKey);

    void revoke(UUID keyId);

    void recordUsage(UUID keyId, double costUsd);

    record VirtualKeyResult(UUID keyId, String rawKey, String keyPrefix) {}
}
