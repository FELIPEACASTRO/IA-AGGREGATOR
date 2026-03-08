package com.ia.aggregator.infrastructure.ai.pricing;

import com.ia.aggregator.application.ai.dto.AiCostEstimate;
import com.ia.aggregator.application.ai.dto.AiUsageEstimate;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@EnableConfigurationProperties(AiPricingProperties.class)
public class MapBackedAiPricingResolver implements AiPricingResolver {

    private final AiPricingProperties pricingProperties;

    public MapBackedAiPricingResolver(AiPricingProperties pricingProperties) {
        this.pricingProperties = pricingProperties;
    }

    @Override
    public AiCostEstimate resolve(String providerId, String model, AiUsageEstimate usage) {
        AiPricingProperties.ProviderPricing providerPricing = pricingProperties.getProviders().get(providerId);
        if (providerPricing == null) {
            return AiCostEstimate.unsupported(providerId, model);
        }

        AiPricingProperties.ModelPricing modelPricing = providerPricing.getModels().get(model);
        if (modelPricing == null
                || modelPricing.getInputPerMillionTokens() == null
                || modelPricing.getOutputPerMillionTokens() == null) {
            return AiCostEstimate.unsupported(providerId, model);
        }

        BigDecimal inputCost = modelPricing.getInputPerMillionTokens()
                .multiply(BigDecimal.valueOf(usage.inputTokens()))
                .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = modelPricing.getOutputPerMillionTokens()
                .multiply(BigDecimal.valueOf(usage.outputTokens()))
                .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP);

        return new AiCostEstimate(
                providerId,
                model,
                modelPricing.getCurrency(),
                inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP),
                true
        );
    }
}
