package com.ia.aggregator.application.platform.port.out;

import com.ia.aggregator.domain.platform.BatchJob;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchJobRepository {
    void save(BatchJob job);
    Optional<BatchJob> findById(UUID id);
    List<BatchJob> findByOrg(UUID orgId, int offset, int limit);
}
