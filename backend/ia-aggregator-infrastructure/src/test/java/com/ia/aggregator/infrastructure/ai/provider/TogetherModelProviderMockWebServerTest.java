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

class TogetherModelProviderMockWebServerTest {

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
                        {"choices":[{"message":{"content":"hello-from-together"}}]}
                        """));

        TogetherModelProvider provider = new TogetherModelProvider(
                new ObjectMapper(),
                CircuitBreakerRegistry.ofDefaults(),
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("meta-llama/Llama-3.1-8B-Instruct-Turbo")
        );

        String response = provider.generate("hello", "meta-llama/Llama-3.1-8B-Instruct-Turbo");

        assertEquals("hello-from-together", response);
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void generate_shouldThrowAfterRetriesExhausted() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"error\":\"down\"}"));

        TogetherModelProvider provider = new TogetherModelProvider(
                new ObjectMapper(),
                CircuitBreakerRegistry.ofDefaults(),
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("meta-llama/Llama-3.1-8B-Instruct-Turbo")
        );

        TechnicalException ex = assertThrows(TechnicalException.class,
                () -> provider.generate("hello", "meta-llama/Llama-3.1-8B-Instruct-Turbo"));

        assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void generate_shouldFailFastWhenCircuitBreakerIsOpen() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("aiProviderTogether").transitionToOpenState();

        TogetherModelProvider provider = new TogetherModelProvider(
                new ObjectMapper(),
                registry,
                "test-key",
                mockWebServer.url("/").toString().replaceAll("/$", ""),
                5_000,
                2,
                10,
                List.of("meta-llama/Llama-3.1-8B-Instruct-Turbo")
        );

        TechnicalException ex = assertThrows(TechnicalException.class,
                () -> provider.generate("hello", "meta-llama/Llama-3.1-8B-Instruct-Turbo"));

        assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(0, mockWebServer.getRequestCount());
    }
}