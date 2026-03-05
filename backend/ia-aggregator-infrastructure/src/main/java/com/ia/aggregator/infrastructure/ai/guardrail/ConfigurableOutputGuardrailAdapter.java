package com.ia.aggregator.infrastructure.ai.guardrail;

import com.ia.aggregator.application.ai.port.out.OutputGuardrailPort;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ConfigurableOutputGuardrailAdapter implements OutputGuardrailPort {

    private enum GuardrailAction {
        BLOCK,
        LOG_ONLY
    }

    private final int maxOutputLength;
    private final List<Pattern> blockedPatterns;
    private final GuardrailAction action;

    public ConfigurableOutputGuardrailAdapter(
            @Value("${app.ai.guardrails.max-output-length:8000}") int maxOutputLength,
            @Value("${app.ai.guardrails.output-blocked-patterns:(?i).*\\b(api\\s*key|private\\s*key|token\\s*:\\s*[A-Za-z0-9-_]{12,})\\b.*}")
            String blockedPattern,
            @Value("${app.ai.guardrails.output-action:block}") String action
    ) {
        this.maxOutputLength = maxOutputLength;
        this.blockedPatterns = toPatterns(blockedPattern);
        this.action = parseAction(action);
    }

    @Override
    public void validate(String output) {
        if (output == null || output.isBlank()) {
            throw new BusinessException(ErrorCode.GEN_002, "AI output cannot be blank");
        }

        if (output.length() > maxOutputLength) {
            throw new BusinessException(ErrorCode.AI_004, "AI output exceeds maximum allowed length");
        }

        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(output).matches()) {
                if (action == GuardrailAction.BLOCK) {
                    throw new BusinessException(ErrorCode.GEN_002, "AI output blocked by guardrail policy");
                }
                return;
            }
        }
    }

    private static GuardrailAction parseAction(String action) {
        if (action == null || action.isBlank()) {
            return GuardrailAction.BLOCK;
        }
        try {
            return GuardrailAction.valueOf(action.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return GuardrailAction.BLOCK;
        }
    }

    private static List<Pattern> toPatterns(String blockedPattern) {
        if (blockedPattern == null || blockedPattern.isBlank()) {
            return List.of();
        }
        return List.of(Pattern.compile(blockedPattern.trim()));
    }
}