package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import reactor.core.publisher.Flux;

/**
 * Use case for streaming chat completions via SSE.
 * Returns a Flux of string tokens as they arrive from the provider.
 */
public interface StreamingChatUseCase {

    /**
     * Execute a chat command and return a stream of tokens.
     *
     * @param command the chat command with prompt and optional model preference
     * @return a Flux of string tokens streamed from the provider
     */
    Flux<String> executeStreaming(ChatCommand command);
}
