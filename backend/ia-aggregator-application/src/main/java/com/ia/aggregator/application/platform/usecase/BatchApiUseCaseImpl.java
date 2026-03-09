package com.ia.aggregator.application.platform.usecase;

import com.ia.aggregator.application.platform.port.in.BatchApiUseCase;
import com.ia.aggregator.application.platform.port.out.BatchJobRepository;
import com.ia.aggregator.domain.platform.BatchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class BatchApiUseCaseImpl implements BatchApiUseCase {

    private static final Logger log = LoggerFactory.getLogger(BatchApiUseCaseImpl.class);
    private static final double BATCH_DISCOUNT = 0.50; // 50% discount

    private final BatchJobRepository batchJobRepository;

    public BatchApiUseCaseImpl(BatchJobRepository batchJobRepository) {
        this.batchJobRepository = batchJobRepository;
    }

    @Override
    public BatchJob submit(UUID orgId, String webhookUrl, List<Map<String, Object>> requests) {
        BatchJob job = new BatchJob(
                UUID.randomUUID(), orgId, webhookUrl,
                requests.size(), 0, 0,
                BatchJob.BatchStatus.QUEUED, 0.0, BATCH_DISCOUNT,
                Instant.now(), null
        );
        batchJobRepository.save(job);
        log.info("Batch job submitted: id={}, requests={}, org={}", job.id(), requests.size(), orgId);

        // In full implementation, would enqueue requests for async processing
        return job;
    }

    @Override
    public BatchJob getStatus(UUID jobId) {
        return batchJobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Batch job not found: " + jobId));
    }

    @Override
    public List<BatchJob> listByOrg(UUID orgId, int page, int size) {
        return batchJobRepository.findByOrg(orgId, page * size, size);
    }

    @Override
    public BatchJob cancel(UUID jobId) {
        BatchJob job = getStatus(jobId);
        BatchJob canceled = new BatchJob(
                job.id(), job.orgId(), job.webhookUrl(),
                job.totalRequests(), job.completedRequests(), job.failedRequests(),
                BatchJob.BatchStatus.CANCELED, job.totalCostUsd(), job.discountPercent(),
                job.createdAt(), Instant.now()
        );
        batchJobRepository.save(canceled);
        log.info("Batch job canceled: id={}", jobId);
        return canceled;
    }
}
