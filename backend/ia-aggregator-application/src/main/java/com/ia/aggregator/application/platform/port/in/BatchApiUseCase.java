package com.ia.aggregator.application.platform.port.in;

import com.ia.aggregator.domain.platform.BatchJob;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for batch API processing.
 */
public interface BatchApiUseCase {

    BatchJob submit(UUID orgId, String webhookUrl, List<Map<String, Object>> requests);

    BatchJob getStatus(UUID jobId);

    List<BatchJob> listByOrg(UUID orgId, int page, int size);

    BatchJob cancel(UUID jobId);
}
