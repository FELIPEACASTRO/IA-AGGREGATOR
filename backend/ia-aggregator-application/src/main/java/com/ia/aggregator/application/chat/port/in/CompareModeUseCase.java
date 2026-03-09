package com.ia.aggregator.application.chat.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Use case for Compare Mode — sends the same prompt to multiple models simultaneously.
 */
public interface CompareModeUseCase {

    /**
     * Send the same prompt to up to 4 models and collect responses.
     */
    List<CompareResult> compare(UUID orgId, UUID userId, String prompt, List<String> models,
                                 String systemInstruction, Double temperature, Integer maxTokens);

    record CompareResult(
            String model,
            String provider,
            String response,
            long latencyMs,
            double costUsd,
            long inputTokens,
            long outputTokens
    ) {}
}
