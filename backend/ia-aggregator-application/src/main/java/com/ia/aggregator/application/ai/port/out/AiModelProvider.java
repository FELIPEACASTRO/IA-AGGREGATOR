package com.ia.aggregator.application.ai.port.out;

public interface AiModelProvider {

    String providerName();

    boolean supports(String model);

    String generate(String prompt, String model);
}
