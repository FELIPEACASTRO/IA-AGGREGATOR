package com.ia.aggregator.application.platform.port.out;

import com.ia.aggregator.domain.platform.VirtualKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VirtualKeyRepository {
    void save(VirtualKey key);
    Optional<VirtualKey> findById(UUID id);
    Optional<VirtualKey> findByKeyHash(String keyHash);
    List<VirtualKey> findByOrg(UUID orgId);
    void delete(UUID id);
}
