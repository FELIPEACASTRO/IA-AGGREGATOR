package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XaiModelProviderMockWebServerTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void generate_shouldRetryAndEventuallySucceed() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("""
                        {"choices":[{"message":{"content":"hello-from-xai"}}]}
                        """));

        XaiModelProvider provider = new XaiModelProvider(
                new ObjectMapper(),
                CircuitBreakerRegistry.ofDefaults(),
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("grok-2-latest")
        );

        String response = provider.generate("hello", "grok-2-latest");

        assertEquals("hello-from-xai", response);
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void generate_shouldThrowAfterRetriesExhausted() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));

        XaiModelProvider provider = new XaiModelProvider(
                new ObjectMapper(),
                CircuitBreakerRegistry.ofDefaults(),
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("grok-2-latest")
        );

        TechnicalException ex = assertThrows(TechnicalException.class,
                () -> provider.generate("hello", "grok-2-latest"));

        assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void generate_shouldFailFastWhenCircuitBreakerIsOpen() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("aiProviderXai").transitionToOpenState();

        XaiModelProvider provider = new XaiModelProvider(
                new ObjectMapper(),
                registry,
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("grok-2-latest")
        );

        TechnicalException ex = assertThrows(TechnicalException.class,
                () -> provider.generate("hello", "grok-2-latest"));

        assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(0, mockWebServer.getRequestCount());
    }
}