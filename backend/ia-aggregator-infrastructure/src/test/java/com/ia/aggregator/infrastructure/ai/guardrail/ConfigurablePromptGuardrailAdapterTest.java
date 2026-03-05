package com.ia.aggregator.infrastructure.ai.guardrail;

import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurablePromptGuardrailAdapterTest {

    @Test
    void validate_shouldThrowWhenBlockedPatternAndActionBlock() {
        ConfigurablePromptGuardrailAdapter adapter = new ConfigurablePromptGuardrailAdapter(
                5000,
            "(?i).*\\b(jailbreak)\\b.*",
                "block"
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> adapter.validate("please jailbreak this"));

        assertEquals(ErrorCode.GEN_002, ex.getErrorCode());
    }

    @Test
    void validate_shouldNotThrowWhenBlockedPatternAndActionLogOnly() {
        ConfigurablePromptGuardrailAdapter adapter = new ConfigurablePromptGuardrailAdapter(
                5000,
            "(?i).*\\b(jailbreak)\\b.*",
                "log-only"
        );

        assertDoesNotThrow(() -> adapter.validate("please jailbreak this"));
    }
}
