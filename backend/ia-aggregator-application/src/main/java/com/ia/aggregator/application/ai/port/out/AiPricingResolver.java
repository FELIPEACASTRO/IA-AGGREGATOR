package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.dto.AiCostEstimate;
import com.ia.aggregator.application.ai.dto.AiUsageEstimate;

public interface AiPricingResolver {

    AiCostEstimate resolve(String providerId, String model, AiUsageEstimate usage);

    static AiPricingResolver noop() {
        return (providerId, model, usage) -> AiCostEstimate.unsupported(providerId, model);
    }
}
