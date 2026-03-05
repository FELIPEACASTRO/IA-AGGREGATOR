package com.ia.aggregator.presentation.analytics;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AnalyticsReportEventResponse(
        UUID id,
        UUID reportId,
        String eventName,
        String eventCategory,
        Instant eventTimestamp,
        Instant createdAt,
        Map<String, Object> metadata
) {
}