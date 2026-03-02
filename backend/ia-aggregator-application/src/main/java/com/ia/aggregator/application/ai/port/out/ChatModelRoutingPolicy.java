package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.dto.ChatCommand;

import java.util.List;

public interface ChatModelRoutingPolicy {
    List<String> resolveOrderedModels(ChatCommand command);
}
