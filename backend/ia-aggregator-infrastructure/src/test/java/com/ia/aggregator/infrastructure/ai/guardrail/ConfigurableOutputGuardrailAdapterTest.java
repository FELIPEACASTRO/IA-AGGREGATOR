package com.ia.aggregator.infrastructure.ai.guardrail;

import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurableOutputGuardrailAdapterTest {

    @Test
    void validate_shouldThrowWhenBlockedPatternAndActionBlock() {
        ConfigurableOutputGuardrailAdapter adapter = new ConfigurableOutputGuardrailAdapter(
                8000,
            "(?i).*\\b(api\\s*key)\\b.*",
                "block"
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> adapter.validate("api key exposed"));

        assertEquals(ErrorCode.GEN_002, ex.getErrorCode());
    }

    @Test
    void validate_shouldNotThrowWhenBlockedPatternAndActionLogOnly() {
        ConfigurableOutputGuardrailAdapter adapter = new ConfigurableOutputGuardrailAdapter(
                8000,
            "(?i).*\\b(api\\s*key)\\b.*",
                "log-only"
        );

        assertDoesNotThrow(() -> adapter.validate("api key exposed"));
    }
}
