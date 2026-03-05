package com.ia.aggregator.application.ai.port.out;

public interface PromptGuardrailPort {
    void validate(String prompt);
}