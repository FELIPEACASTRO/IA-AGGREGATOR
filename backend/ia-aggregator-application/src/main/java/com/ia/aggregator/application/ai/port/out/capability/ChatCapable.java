package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;

/**
 * Capability interface for text-to-text chat completion.
 * Providers implementing this can process conversational prompts.
 */
public interface ChatCapable {

    /**
     * Executes a chat completion request.
     *
     * @param request the chat request containing prompt, model, and parameters
     * @return the chat result with generated content and metadata
     */
    ChatResult chat(ChatRequest request);
}
