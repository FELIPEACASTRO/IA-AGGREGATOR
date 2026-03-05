package com.ia.aggregator.presentation.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.presentation.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

        @Mock
        private AnalyticsIngestionService analyticsIngestionService;

        @InjectMocks
        private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ingestEvents_shouldReturn202WhenPayloadIsValid() throws Exception {
        AnalyticsReportRequest request = new AnalyticsReportRequest(
                "frontend",
                "2026-03-04T22:00:00Z",
                1,
                Map.of("chat_send_start", 1),
                List.of(new AnalyticsEventRequest(
                        "chat_send_start",
                        "2026-03-04T22:00:00Z",
                        Map.of("model", "gpt-4o-mini")
                ))
        );

        mockMvc.perform(post("/api/v1/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Analytics report received"));

        verify(analyticsIngestionService).ingest(any(AnalyticsReportRequest.class));
    }

    @Test
    void ingestEvents_shouldReturn400WhenPayloadIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "source": "",
                  "generatedAt": "",
                  "totalEvents": -1,
                  "counters": null,
                  "events": []
                }
                """;

        mockMvc.perform(post("/api/v1/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void listReports_shouldReturn200WithPersistedReports() throws Exception {
        when(analyticsIngestionService.listReports(20, 0, null, null, null, null)).thenReturn(List.of(
                new AnalyticsReportSummaryResponse(
                        UUID.randomUUID(),
                        "frontend",
                        null,
                        null,
                        4,
                        Map.of("chat_send_start", 2)
                )
        ));

        mockMvc.perform(get("/api/v1/analytics/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].source").value("frontend"))
                .andExpect(jsonPath("$.data[0].totalEvents").value(4));
    }

    @Test
    void listReports_shouldPassDateRangeAndSortingQueryParams() throws Exception {
        when(analyticsIngestionService.listReports(10, 2, "2026-03-01T00:00:00Z", "2026-03-02T23:59:59Z", "totalEvents", "asc"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analytics/reports")
                        .param("limit", "10")
                        .param("page", "2")
                        .param("from", "2026-03-01T00:00:00Z")
                        .param("to", "2026-03-02T23:59:59Z")
                        .param("sortBy", "totalEvents")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listReportEvents_shouldReturn200WithPersistedEvents() throws Exception {
        UUID reportId = UUID.randomUUID();
        when(analyticsIngestionService.listReportEvents(reportId, 50, 0, null)).thenReturn(List.of(
                new AnalyticsReportEventResponse(
                        UUID.randomUUID(),
                        reportId,
                        "chat_send_start",
                        "chat",
                        null,
                        null,
                        Map.of("model", "gpt-4o-mini")
                )
        ));

        mockMvc.perform(get("/api/v1/analytics/reports/{reportId}/events", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventName").value("chat_send_start"))
                .andExpect(jsonPath("$.data[0].eventCategory").value("chat"));
    }

        @Test
        void listReportEvents_shouldPassPaginationAndCategoryQueryParams() throws Exception {
                UUID reportId = UUID.randomUUID();
                when(analyticsIngestionService.listReportEvents(reportId, 25, 50, "chat")).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/analytics/reports/{reportId}/events", reportId)
                                                .param("limit", "25")
                                                .param("offset", "50")
                                                .param("category", "chat"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }
}
