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

class GroqModelProviderMockWebServerTest {

    private MockWebServer mockWebServer;
    private GroqModelProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("aiProviderGroq");

        provider = new GroqModelProvider(
                new ObjectMapper(),
                registry,
                "test-api-key",
                mockWebServer.url("/openai/v1").toString().replaceAll("/$", ""),
                5_000,
                2,
                1,
                List.of("llama-3.1-8b-instant")
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldRetryAndSucceedOnSecondAttempt() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          \"choices\": [
                            {
                              \"message\": {
                                \"content\": \"Groq recovered\"
                              }
                            }
                          ]
                        }
                        """));

        String response = provider.generate("Hello", "llama-3.1-8b-instant");

        assertEquals("Groq recovered", response);
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void shouldFailAfterRetriesExhausted() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

      TechnicalException ex = assertThrows(TechnicalException.class,
        () -> provider.generate("Hello", "llama-3.1-8b-instant"));

      assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void shouldFailFastWhenCircuitBreakerIsOpen() {
      CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
      registry.circuitBreaker("aiProviderGroq").transitionToOpenState();

        GroqModelProvider providerWithOpenCircuit = new GroqModelProvider(
                new ObjectMapper(),
        registry,
                "test-api-key",
                mockWebServer.url("/openai/v1").toString().replaceAll("/$", ""),
                5_000,
                2,
                1,
                List.of("llama-3.1-8b-instant")
        );

              TechnicalException ex = assertThrows(TechnicalException.class,
                () -> providerWithOpenCircuit.generate("Hello", "llama-3.1-8b-instant"));

              assertEquals(ErrorCode.AI_002, ex.getErrorCode());
        assertEquals(0, mockWebServer.getRequestCount());
    }
}