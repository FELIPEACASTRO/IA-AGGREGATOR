package com.ia.aggregator.infrastructure.ai.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.ai.pricing")
public class AiPricingProperties {

    private Map<String, ProviderPricing> providers = new HashMap<>();

    public Map<String, ProviderPricing> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderPricing> providers) {
        this.providers = providers;
    }

    public static class ProviderPricing {
        private Map<String, ModelPricing> models = new HashMap<>();

        public Map<String, ModelPricing> getModels() {
            return models;
        }

        public void setModels(Map<String, ModelPricing> models) {
            this.models = models;
        }
    }

    public static class ModelPricing {
        private BigDecimal inputPerMillionTokens;
        private BigDecimal outputPerMillionTokens;
        private String currency = "USD";

        public BigDecimal getInputPerMillionTokens() {
            return inputPerMillionTokens;
        }

        public void setInputPerMillionTokens(BigDecimal inputPerMillionTokens) {
            this.inputPerMillionTokens = inputPerMillionTokens;
        }

        public BigDecimal getOutputPerMillionTokens() {
            return outputPerMillionTokens;
        }

        public void setOutputPerMillionTokens(BigDecimal outputPerMillionTokens) {
            this.outputPerMillionTokens = outputPerMillionTokens;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
