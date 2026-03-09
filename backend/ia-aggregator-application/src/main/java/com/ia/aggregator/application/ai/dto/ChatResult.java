package com.ia.aggregator.application.ai.dto;

/**
 * Result DTO for chat completion.
 *
 * @param content generated text content
 * @param modelUsed actual model used
 * @param providerUsed provider that generated the response
 * @param fallbackUsed whether a fallback provider/model was used
 * @param attempts number of attempts before success
 * @param promptTokens input token count (null if not reported)
 * @param completionTokens output token count (null if not reported)
 * @param finishReason reason generation stopped (e.g., "stop", "length")
 */
public record ChatResult(
        String content,
        String modelUsed,
        String providerUsed,
        boolean fallbackUsed,
        int attempts,
        Integer promptTokens,
        Integer completionTokens,
        String finishReason
) {
    /** Convenience constructor matching the existing ChatResponse fields. */
    public ChatResult(String content, String modelUsed, String providerUsed, boolean fallbackUsed, int attempts) {
        this(content, modelUsed, providerUsed, fallbackUsed, attempts, null, null, null);
    }
}
