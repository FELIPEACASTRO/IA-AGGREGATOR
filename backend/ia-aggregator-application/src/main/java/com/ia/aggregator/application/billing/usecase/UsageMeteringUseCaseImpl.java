package com.ia.aggregator.application.billing.usecase;

import com.ia.aggregator.application.billing.port.in.UsageMeteringUseCase;
import com.ia.aggregator.application.billing.port.out.UsageRepository;
import com.ia.aggregator.domain.billing.UsageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsageMeteringUseCaseImpl implements UsageMeteringUseCase {

    private static final Logger log = LoggerFactory.getLogger(UsageMeteringUseCaseImpl.class);

    private final UsageRepository usageRepository;

    public UsageMeteringUseCaseImpl(UsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    @Override
    public void recordUsage(UsageRecord record) {
        usageRepository.save(record);
        log.debug("Usage recorded: org={}, provider={}, model={}, cost={}",
                record.orgId(), record.provider(), record.model(), record.costUsd());
    }

    @Override
    public List<UsageRecord> getUsage(UUID orgId, Instant from, Instant to) {
        return usageRepository.findByOrg(orgId, from, to);
    }

    @Override
    public Map<String, UsageSummary> getUsageSummary(UUID orgId, Instant from, Instant to) {
        List<UsageRecord> records = usageRepository.findByOrg(orgId, from, to);
        return records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.provider() + ":" + r.model(),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            String key = list.getFirst().provider() + ":" + list.getFirst().model();
                            return new UsageSummary(
                                    key,
                                    list.size(),
                                    list.stream().mapToLong(UsageRecord::inputTokens).sum(),
                                    list.stream().mapToLong(UsageRecord::outputTokens).sum(),
                                    list.stream().mapToDouble(UsageRecord::costUsd).sum()
                            );
                        })
                ));
    }

    @Override
    public double getTotalCost(UUID orgId, Instant from, Instant to) {
        return usageRepository.sumCostByOrg(orgId, from, to);
    }
}
