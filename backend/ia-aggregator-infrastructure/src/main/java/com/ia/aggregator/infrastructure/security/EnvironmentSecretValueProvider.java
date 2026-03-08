package com.ia.aggregator.infrastructure.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EnvironmentSecretValueProvider implements SecretValueProvider {

    private final Environment environment;

    public EnvironmentSecretValueProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Optional<String> getSecret(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
