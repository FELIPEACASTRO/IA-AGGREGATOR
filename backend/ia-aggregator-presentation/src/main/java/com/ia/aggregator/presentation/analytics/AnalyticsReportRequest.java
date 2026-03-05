package com.ia.aggregator.presentation.analytics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record AnalyticsReportRequest(
        @NotBlank(message = "source is required")
        String source,

        @NotBlank(message = "generatedAt is required")
        String generatedAt,

        @PositiveOrZero(message = "totalEvents must be >= 0")
        int totalEvents,

        @NotNull(message = "counters is required")
        Map<String, Integer> counters,

        @NotEmpty(message = "events must not be empty")
        @Size(max = 2000, message = "events size must be <= 2000")
        List<@Valid AnalyticsEventRequest> events
) {
}
