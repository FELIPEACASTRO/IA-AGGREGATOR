package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("real-ai")
@EnabledIfEnvironmentVariable(named = "RUN_REAL_AI_TESTS", matches = "(?i)true")
class OfficialProvidersRealSmokeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final CircuitBreakerRegistry CIRCUIT_BREAKER_REGISTRY = CircuitBreakerRegistry.ofDefaults();
    private static final long TIMEOUT_MS = 60_000L;
    private static final int RETRY_ATTEMPTS = 1;
    private static final long RETRY_BACKOFF_MS = 250L;
    private static final String PROMPT = "Responda apenas com OK.";

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void openAi_realSmoke_shouldReturnContent() {
        String apiKey = requireEnv("OPENAI_API_KEY");
        String model = envOrDefault("OPENAI_SUPPORTED_MODELS", "gpt-4o-mini").split(",")[0].trim();
        OpenAiModelProvider provider = new OpenAiModelProvider(
                OBJECT_MAPPER,
                CIRCUIT_BREAKER_REGISTRY,
                apiKey,
                envOrDefault("OPENAI_BASE_URL", "https://api.openai.com"),
                TIMEOUT_MS,
                RETRY_ATTEMPTS,
                RETRY_BACKOFF_MS,
                List.of(model)
        );

        assertResponse(provider.sendPrompt(promptRequest("openai", model)));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void gemini_realSmoke_shouldReturnContent() {
        String apiKey = requireEnv("GEMINI_API_KEY");
        String model = envOrDefault("GEMINI_SUPPORTED_MODELS", "gemini-1.5-flash").split(",")[0].trim();
        GeminiModelProvider provider = new GeminiModelProvider(
                OBJECT_MAPPER,
                CIRCUIT_BREAKER_REGISTRY,
                apiKey,
                envOrDefault("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com"),
                TIMEOUT_MS,
                RETRY_ATTEMPTS,
                RETRY_BACKOFF_MS,
                List.of(model)
        );

        assertResponse(provider.sendPrompt(promptRequest("gemini", model)));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void deepSeek_realSmoke_shouldReturnContent() {
        String apiKey = requireEnv("DEEPSEEK_API_KEY");
        String model = envOrDefault("DEEPSEEK_SUPPORTED_MODELS", "deepseek-chat").split(",")[0].trim();
        DeepSeekModelProvider provider = new DeepSeekModelProvider(
                OBJECT_MAPPER,
                CIRCUIT_BREAKER_REGISTRY,
                apiKey,
                envOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                TIMEOUT_MS,
                RETRY_ATTEMPTS,
                RETRY_BACKOFF_MS,
                List.of(model)
        );

        assertResponse(provider.sendPrompt(promptRequest("deepseek", model)));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void anthropic_realSmoke_shouldReturnContent() {
        String apiKey = requireEnv("ANTHROPIC_API_KEY");
        String model = envOrDefault("ANTHROPIC_SUPPORTED_MODELS", "claude-3-5-haiku-latest").split(",")[0].trim();
        AnthropicModelProvider provider = new AnthropicModelProvider(
                OBJECT_MAPPER,
                CIRCUIT_BREAKER_REGISTRY,
                apiKey,
                envOrDefault("ANTHROPIC_BASE_URL", "https://api.anthropic.com"),
                TIMEOUT_MS,
                RETRY_ATTEMPTS,
                RETRY_BACKOFF_MS,
                800,
                List.of(model)
        );

        assertResponse(provider.sendPrompt(promptRequest("anthropic", model)));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void xai_realSmoke_shouldReturnContent() {
        String apiKey = requireEnv("XAI_API_KEY");
        String model = envOrDefault("XAI_SUPPORTED_MODELS", "grok-3-latest").split(",")[0].trim();
        XaiModelProvider provider = new XaiModelProvider(
                OBJECT_MAPPER,
                CIRCUIT_BREAKER_REGISTRY,
                apiKey,
                envOrDefault("XAI_BASE_URL", "https://api.x.ai/v1"),
                TIMEOUT_MS,
                RETRY_ATTEMPTS,
                RETRY_BACKOFF_MS,
                List.of(model)
        );

        assertResponse(provider.sendPrompt(promptRequest("xai", model)));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void perplexity_realSmoke_shouldReturnContent() {
        String apiKey = requireEnv("PERPLEXITY_API_KEY");
        String model = envOrDefault("PERPLEXITY_SUPPORTED_MODELS", "sonar").split(",")[0].trim();
        PerplexityModelProvider provider = new PerplexityModelProvider(
                OBJECT_MAPPER,
                CIRCUIT_BREAKER_REGISTRY,
                apiKey,
                envOrDefault("PERPLEXITY_BASE_URL", "https://api.perplexity.ai"),
                TIMEOUT_MS,
                RETRY_ATTEMPTS,
                RETRY_BACKOFF_MS,
                List.of(model)
        );

        assertResponse(provider.sendPrompt(promptRequest("perplexity", model)));
    }

    private static AiPromptRequest promptRequest(String provider, String model) {
        return new AiPromptRequest(
                "real-smoke-" + provider,
                PROMPT,
                provider,
                model,
                null,
                null,
                null,
                false,
                Map.of("suite", "real-smoke"),
                List.of()
        );
    }

    private static void assertResponse(AiPromptResponse response) {
        assertNotNull(response);
        assertNotNull(response.requestId());
        assertFalse(response.content().isBlank());
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        assumeTrue(value != null && !value.isBlank(), () -> "Ignorando smoke real: variavel ausente " + key);
        return value.trim();
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
