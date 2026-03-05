package com.ia.aggregator.infrastructure.ai.guardrail;

import com.ia.aggregator.application.ai.port.out.PromptGuardrailPort;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ConfigurablePromptGuardrailAdapter implements PromptGuardrailPort {

    private enum GuardrailAction {
        BLOCK,
        LOG_ONLY
    }

    private final int maxPromptLength;
    private final List<Pattern> blockedPatterns;
    private final GuardrailAction action;

    public ConfigurablePromptGuardrailAdapter(
            @Value("${app.ai.guardrails.max-prompt-length:5000}") int maxPromptLength,
            @Value("${app.ai.guardrails.blocked-patterns:(?i).*\\b(ignore\\s+previous\\s+instructions|system\\s+override|jailbreak|bypass\\s+policy)\\b.*}")
            String blockedPattern,
            @Value("${app.ai.guardrails.prompt-action:block}") String action
    ) {
        this.maxPromptLength = maxPromptLength;
        this.blockedPatterns = toPatterns(blockedPattern);
        this.action = parseAction(action);
    }

    @Override
    public void validate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(ErrorCode.GEN_002, "Prompt cannot be blank");
        }

        if (prompt.length() > maxPromptLength) {
            throw new BusinessException(ErrorCode.AI_004, "Prompt exceeds maximum allowed length");
        }

        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(prompt).matches()) {
                if (action == GuardrailAction.BLOCK) {
                    throw new BusinessException(ErrorCode.GEN_002, "Prompt blocked by guardrail policy");
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