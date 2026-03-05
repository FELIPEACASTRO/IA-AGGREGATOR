package com.ia.aggregator.presentation.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.infrastructure.analytics.persistence.entity.AnalyticsEventReportJpaEntity;
import com.ia.aggregator.infrastructure.analytics.persistence.entity.AnalyticsIngestedEventJpaEntity;
import com.ia.aggregator.infrastructure.analytics.persistence.repository.AnalyticsEventReportJpaRepository;
import com.ia.aggregator.infrastructure.analytics.persistence.repository.AnalyticsIngestedEventJpaRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsIngestionServiceTest {

    @Mock
    private AnalyticsEventReportJpaRepository reportRepository;

    @Mock
    private AnalyticsIngestedEventJpaRepository eventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AnalyticsIngestionService analyticsIngestionService;

    @Test
    void ingest_shouldPersistReportAndEvents() throws Exception {
        AnalyticsReportRequest request = new AnalyticsReportRequest(
                "frontend",
                "2026-03-04T22:00:00Z",
                2,
                Map.of("chat_send_start", 2),
                List.of(
                        new AnalyticsEventRequest("chat_send_start", "2026-03-04T22:00:01Z", Map.of("model", "gpt-4o-mini")),
                        new AnalyticsEventRequest("auth_login_success", "2026-03-04T22:00:02Z", null)
                )
        );

        when(reportRepository.save(any(AnalyticsEventReportJpaEntity.class))).thenAnswer(invocation -> {
            AnalyticsEventReportJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        analyticsIngestionService.ingest(request);

        verify(reportRepository).save(any(AnalyticsEventReportJpaEntity.class));

        ArgumentCaptor<List<AnalyticsIngestedEventJpaEntity>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(eventsCaptor.capture());

        List<AnalyticsIngestedEventJpaEntity> savedEvents = eventsCaptor.getValue();
        assertEquals(2, savedEvents.size());
        assertEquals("chat", savedEvents.get(0).getEventCategory());
        assertEquals("auth", savedEvents.get(1).getEventCategory());
        assertNull(savedEvents.get(1).getMetadata());
        assertNotNull(savedEvents.get(0).getEventTimestamp());
    }

    @Test
    void listReports_shouldMapPersistedReports() throws Exception {
        AnalyticsEventReportJpaEntity report = new AnalyticsEventReportJpaEntity();
        report.setId(UUID.randomUUID());
        report.setSource("frontend");
        report.setTotalEvents(3);
        report.setCounters("{\"chat_send_start\":2}");

        when(reportRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        List<AnalyticsReportSummaryResponse> results = analyticsIngestionService.listReports(250);

        assertEquals(1, results.size());
        assertEquals("frontend", results.get(0).source());
        assertEquals(3, results.get(0).totalEvents());
        assertEquals(2, results.get(0).counters().get("chat_send_start"));
        verify(reportRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "receivedAt"))));
    }

    @Test
    void listReports_shouldApplyDateRangeAndSort() {
        when(reportRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        analyticsIngestionService.listReports(
                20,
                0,
                "2026-03-01T00:00:00Z",
                "2026-03-02T23:59:59Z",
                "totalEvents",
                "asc"
        );

        verify(reportRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "totalEvents"))));
    }

    @Test
    void listReports_shouldClampNegativePageToZero() {
        when(reportRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        analyticsIngestionService.listReports(20, -3, null, null, null, null);

        verify(reportRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "receivedAt"))));
    }

    @Test
    void listReportEvents_shouldMapPersistedEvents() {
        UUID reportId = UUID.randomUUID();
        AnalyticsIngestedEventJpaEntity event = new AnalyticsIngestedEventJpaEntity();
        event.setId(UUID.randomUUID());
        event.setReportId(reportId);
        event.setEventName("chat_send_start");
        event.setEventCategory("chat");
        event.setMetadata("{\"model\":\"gpt-4o-mini\",\"tokens\":123}");

        when(eventRepository.findReportEvents(any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(List.of(event));

        List<AnalyticsReportEventResponse> results = analyticsIngestionService.listReportEvents(reportId, 700, -20, "CHAT");

        assertEquals(1, results.size());
        assertEquals(reportId, results.get(0).reportId());
        assertEquals("chat_send_start", results.get(0).eventName());
        assertEquals("chat", results.get(0).eventCategory());
        assertEquals("gpt-4o-mini", results.get(0).metadata().get("model"));
        assertEquals(123, ((Number) results.get(0).metadata().get("tokens")).intValue());
        verify(eventRepository).findReportEvents(reportId, "chat", 200, 0);
    }

    @Test
    void listReportEvents_shouldIgnoreInvalidCategoryFilter() {
        UUID reportId = UUID.randomUUID();
        when(eventRepository.findReportEvents(any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of());

        analyticsIngestionService.listReportEvents(reportId, 20, 40, "unknown");

        verify(eventRepository).findReportEvents(reportId, null, 20, 40);
    }
}
