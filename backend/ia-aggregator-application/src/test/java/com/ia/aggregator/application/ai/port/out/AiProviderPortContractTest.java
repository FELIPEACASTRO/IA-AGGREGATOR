package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.dto.AiHealthStatus;
import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiProviderPortContractTest {

    @Test
    void defaultSendPromptBridgesLegacyGenerateContract() {
        AiModelProvider provider = new AiModelProvider() {
            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public boolean supports(String model) {
                return "stub-model".equals(model);
            }

            @Override
            public String generate(String prompt, String model) {
                return prompt + "::" + model;
            }

            @Override
            public List<String> supportedModels() {
                return List.of("stub-model");
            }
        };

        AiPromptResponse response = provider.sendPrompt(new AiPromptRequest("req-1", "hello", null, "stub-model"));

        assertEquals("hello::stub-model", response.content());
        assertEquals("stub", response.providerUsed());
        assertEquals("stub-model", response.modelUsed());
    }

    @Test
    void defaultHealthCheckExposesBasicMetadata() {
        AiModelProvider provider = new AiModelProvider() {
            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public boolean supports(String model) {
                return true;
            }

            @Override
            public String generate(String prompt, String model) {
                return "ok";
            }

            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        AiHealthStatus healthStatus = provider.healthCheck();

        assertEquals("stub", healthStatus.providerId());
        assertFalse(healthStatus.configured());
        assertFalse(healthStatus.supportsStreaming());
    }
}
