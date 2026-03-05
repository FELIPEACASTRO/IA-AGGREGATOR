package com.ia.aggregator.presentation.analytics;

import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsIngestionService analyticsIngestionService;

    public AnalyticsController(AnalyticsIngestionService analyticsIngestionService) {
        this.analyticsIngestionService = analyticsIngestionService;
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Void>> ingestEvents(@Valid @RequestBody AnalyticsReportRequest request) {
        analyticsIngestionService.ingest(request);
        log.info(
                "Analytics report received: source={}, generatedAt={}, totalEvents={}, distinctCounters={}",
                request.source(),
                request.generatedAt(),
                request.totalEvents(),
                request.counters().size()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Analytics report received"));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<AnalyticsReportSummaryResponse>>> listReports(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", required = false) String sortDir
    ) {
        List<AnalyticsReportSummaryResponse> reports = analyticsIngestionService.listReports(limit, page, from, to, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @GetMapping("/reports/{reportId}/events")
    public ResponseEntity<ApiResponse<List<AnalyticsReportEventResponse>>> listReportEvents(
            @PathVariable UUID reportId,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "category", required = false) String category
    ) {
        List<AnalyticsReportEventResponse> events = analyticsIngestionService.listReportEvents(reportId, limit, offset, category);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
}
