package com.ia.aggregator.presentation.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.infrastructure.analytics.persistence.entity.AnalyticsEventReportJpaEntity;
import com.ia.aggregator.infrastructure.analytics.persistence.entity.AnalyticsIngestedEventJpaEntity;
import com.ia.aggregator.infrastructure.analytics.persistence.repository.AnalyticsEventReportJpaRepository;
import com.ia.aggregator.infrastructure.analytics.persistence.repository.AnalyticsIngestedEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsIngestionService {

    private final AnalyticsEventReportJpaRepository reportRepository;
    private final AnalyticsIngestedEventJpaRepository eventRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsIngestionService(
            AnalyticsEventReportJpaRepository reportRepository,
            AnalyticsIngestedEventJpaRepository eventRepository,
            ObjectMapper objectMapper
    ) {
        this.reportRepository = reportRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingest(AnalyticsReportRequest request) {
        AnalyticsEventReportJpaEntity report = new AnalyticsEventReportJpaEntity();
        report.setSource(request.source());
        report.setGeneratedAt(parseInstant(request.generatedAt()));
        report.setTotalEvents(request.totalEvents());
        report.setCounters(toJson(request.counters()));
        report.setRawPayload(toJson(Map.of(
                "source", request.source(),
                "generatedAt", request.generatedAt(),
                "totalEvents", request.totalEvents(),
                "counters", request.counters(),
                "events", request.events()
        )));
        report.setReceivedAt(Instant.now());

        AnalyticsEventReportJpaEntity savedReport = reportRepository.save(report);
        UUID reportId = savedReport.getId();

        List<AnalyticsIngestedEventJpaEntity> entities = request.events().stream().map(event -> {
            AnalyticsIngestedEventJpaEntity entity = new AnalyticsIngestedEventJpaEntity();
            entity.setReportId(reportId);
            entity.setEventName(event.event());
            entity.setEventCategory(resolveCategory(event.event()));
            entity.setEventTimestamp(parseInstant(event.timestamp()));
            entity.setMetadata(event.metadata() == null ? null : toJson(event.metadata()));
            return entity;
        }).toList();

        eventRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsReportSummaryResponse> listReports(int limit) {
        return listReports(limit, 0, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsReportSummaryResponse> listReports(
            int limit,
            String from,
            String to,
            String sortBy,
            String sortDirection
    ) {
        return listReports(limit, 0, from, to, sortBy, sortDirection);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsReportSummaryResponse> listReports(
            int limit,
            int page,
            String from,
            String to,
            String sortBy,
            String sortDirection
    ) {
        int normalizedLimit = Math.clamp(limit, 1, 100);
        int normalizedPage = Math.max(page, 0);
        Sort sort = resolveReportSort(sortBy, sortDirection);

        Instant fromInstant = parseInstant(from);
        Instant toInstant = parseInstant(to);

        Specification<AnalyticsEventReportJpaEntity> spec = Specification.where(null);
        if (fromInstant != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("receivedAt"), fromInstant));
        }
        if (toInstant != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("receivedAt"), toInstant));
        }

        return reportRepository.findAll(spec, PageRequest.of(normalizedPage, normalizedLimit, sort))
                .stream()
                .map(report -> new AnalyticsReportSummaryResponse(
                        report.getId(),
                        report.getSource(),
                        report.getGeneratedAt(),
                        report.getReceivedAt(),
                        report.getTotalEvents(),
                        parseCounters(report.getCounters())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsReportEventResponse> listReportEvents(UUID reportId, int limit, int offset, String category) {
        int normalizedLimit = Math.clamp(limit, 1, 200);
        int normalizedOffset = Math.max(offset, 0);
        String normalizedCategory = normalizeCategory(category);

        return eventRepository.findReportEvents(reportId, normalizedCategory, normalizedLimit, normalizedOffset)
                .stream()
                .map(event -> new AnalyticsReportEventResponse(
                        event.getId(),
                        event.getReportId(),
                        event.getEventName(),
                        event.getEventCategory(),
                        event.getEventTimestamp(),
                        event.getCreatedAt(),
                        parseMetadata(event.getMetadata())
                ))
                .toList();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize analytics payload", ex);
        }
    }

    private Map<String, Integer> parseCounters(String countersJson) {
        if (countersJson == null || countersJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(
                    countersJson,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class)
            );
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(
                    metadataJson,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }

    private String resolveCategory(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return "system";
        }
        if (eventName.startsWith("auth_")) {
            return "auth";
        }
        if (eventName.startsWith("chat_")) {
            return "chat";
        }
        if (eventName.startsWith("billing_")) {
            return "billing";
        }
        if (eventName.startsWith("settings_") || eventName.startsWith("library_") || eventName.startsWith("prompts_")) {
            return "content";
        }
        return "system";
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim().toLowerCase();
        return switch (normalized) {
            case "auth", "chat", "billing", "content", "system" -> normalized;
            default -> null;
        };
    }

    private Sort resolveReportSort(String sortBy, String sortDirection) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, normalizedSortBy);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "receivedAt";
        }
        return switch (sortBy.trim()) {
            case "receivedAt", "generatedAt", "totalEvents", "source" -> sortBy.trim();
            default -> "receivedAt";
        };
    }
}
