package com.ia.aggregator.infrastructure.ai.routing;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.port.out.ChatModelRoutingPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigurableChatModelRoutingPolicy implements ChatModelRoutingPolicy {

    private final String defaultModel;
    private final List<String> fallbackModels;

    public ConfigurableChatModelRoutingPolicy(
            @Value("${app.ai.routing.default-model:gpt-4o-mini}") String defaultModel,
            @Value("${app.ai.routing.fallback-models:claude-3-5-haiku,gemini-1.5-flash}") List<String> fallbackModels
    ) {
        this.defaultModel = defaultModel;
        this.fallbackModels = fallbackModels;
    }

    @Override
    public List<String> resolveOrderedModels(ChatCommand command) {
        List<String> orderedModels = new ArrayList<>();

        String preferred = command.preferredModel();
        if (preferred != null && !preferred.isBlank()) {
            orderedModels.add(preferred.trim());
        } else {
            orderedModels.add(defaultModel);
        }

        for (String fallback : fallbackModels) {
            String normalized = fallback == null ? "" : fallback.trim();
            if (!normalized.isBlank() && !orderedModels.contains(normalized)) {
                orderedModels.add(normalized);
            }
        }

        return orderedModels;
    }
}
