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

class DeepSeekModelProviderMockWebServerTest {

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
                        {"choices":[{"message":{"content":"hello-from-deepseek"}}]}
                        """));

        DeepSeekModelProvider provider = new DeepSeekModelProvider(
                new ObjectMapper(),
                CircuitBreakerRegistry.ofDefaults(),
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("deepseek-chat")
        );

        String response = provider.generate("hello", "deepseek-chat");

        assertEquals("hello-from-deepseek", response);
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void generate_shouldThrowAfterRetriesExhausted() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));

        DeepSeekModelProvider provider = new DeepSeekModelProvider(
                new ObjectMapper(),
                CircuitBreakerRegistry.ofDefaults(),
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("deepseek-chat")
        );

        TechnicalException ex = assertThrows(TechnicalException.class,
                () -> provider.generate("hello", "deepseek-chat"));

        assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void generate_shouldFailFastWhenCircuitBreakerIsOpen() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("aiProviderDeepseek").transitionToOpenState();

        DeepSeekModelProvider provider = new DeepSeekModelProvider(
                new ObjectMapper(),
                registry,
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("deepseek-chat")
        );

        TechnicalException ex = assertThrows(TechnicalException.class,
                () -> provider.generate("hello", "deepseek-chat"));

        assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(0, mockWebServer.getRequestCount());
    }
}
