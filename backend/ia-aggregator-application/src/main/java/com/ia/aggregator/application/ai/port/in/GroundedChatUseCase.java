package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.GroundedChatRequest;
import com.ia.aggregator.application.ai.dto.GroundedChatResult;

/**
 * Input port for web-grounded chat.
 * Generates responses backed by real-time web search results.
 */
public interface GroundedChatUseCase {
    GroundedChatResult execute(GroundedChatRequest request);
}
