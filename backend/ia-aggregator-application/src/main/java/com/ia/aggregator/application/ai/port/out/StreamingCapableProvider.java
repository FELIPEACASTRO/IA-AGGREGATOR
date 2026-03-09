package com.ia.aggregator.application.ai.port.out;

import reactor.core.publisher.Flux;

/**
 * Provider interface for streaming chat completions.
 * Providers that support token-by-token streaming should implement this.
 */
public interface StreamingCapableProvider {

    /**
     * Generate a streaming response for the given prompt and model.
     *
     * @param prompt the user prompt
     * @param model the model identifier
     * @return a Flux of string tokens
     */
    Flux<String> generateStream(String prompt, String model);
}
