package com.ia.aggregator.infrastructure.security;

import java.util.Optional;

public interface SecretValueProvider {

    Optional<String> getSecret(String key);

    default boolean hasSecret(String key) {
        return getSecret(key).isPresent();
    }
}
