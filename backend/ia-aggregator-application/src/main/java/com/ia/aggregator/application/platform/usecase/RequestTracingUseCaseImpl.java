package com.ia.aggregator.application.platform.usecase;

import com.ia.aggregator.application.platform.port.in.RequestTracingUseCase;
import com.ia.aggregator.application.platform.port.out.RequestTraceRepository;
import com.ia.aggregator.domain.platform.RequestTrace;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RequestTracingUseCaseImpl implements RequestTracingUseCase {

    private final RequestTraceRepository traceRepository;

    public RequestTracingUseCaseImpl(RequestTraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    @Override
    public void record(RequestTrace trace) {
        traceRepository.save(trace);
    }

    @Override
    public List<RequestTrace> listByOrg(UUID orgId, Instant from, Instant to, int page, int size) {
        return traceRepository.findByOrg(orgId, from, to, page * size, size);
    }

    @Override
    public List<RequestTrace> listByVirtualKey(UUID virtualKeyId, Instant from, Instant to,
                                                int page, int size) {
        return traceRepository.findByVirtualKey(virtualKeyId, from, to, page * size, size);
    }

    @Override
    public TracingSummary getSummary(UUID orgId, Instant from, Instant to) {
        long total = traceRepository.countByOrg(orgId, from, to);
        long success = traceRepository.countSuccessByOrg(orgId, from, to);
        long failed = total - success;
        double cost = traceRepository.sumCostByOrg(orgId, from, to);

        return new TracingSummary(total, success, failed, 0, cost, 0, 0, 0);
    }
}
