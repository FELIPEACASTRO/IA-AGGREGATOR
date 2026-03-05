package com.ia.aggregator.application.ai.port.out;

public interface OutputGuardrailPort {
    void validate(String output);
}