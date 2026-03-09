package com.ia.aggregator.application.ai.port.out;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * SPI for normalizing raw provider JSON responses into typed application DTOs.
 *
 * <p>Different providers return the same conceptual result in different shapes.
 * This normalizer abstracts that difference, allowing use cases to operate on
 * uniform DTOs regardless of provider-specific response structure.
 *
 * <p>Example: OpenAI returns {@code choices[0].message.content} while Anthropic
 * returns {@code content[0].text} — both normalize to {@code ChatResult.text()}.
 *
 * <p>Big O: O(1) for flat JSON parsing; O(N) for arrays where N = result count.
 *
 * @param <T> the target DTO type
 */
public interface ProviderResponseNormalizer<T> {

    /**
     * Normalizes a raw provider JSON response node into the target DTO type.
     *
     * @param rawResponse the root JsonNode of the provider's HTTP response body
     * @return the normalized DTO, never null
     * @throws com.ia.aggregator.common.exception.TechnicalException with failureCategory=parsing
     *         if the response cannot be parsed into the target type
     */
    T normalize(JsonNode rawResponse);
}
