package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.dto.ChatResponse;

public interface ChatUseCase {
    ChatResponse execute(ChatCommand command);
}
