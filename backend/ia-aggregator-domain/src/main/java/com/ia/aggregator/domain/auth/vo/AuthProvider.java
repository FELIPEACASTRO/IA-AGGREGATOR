package com.ia.aggregator.domain.auth.vo;

import java.util.Arrays;

/**
 * Authentication providers supported.
 */
public enum AuthProvider {
    LOCAL("email"),
    GOOGLE("google"),
    GITHUB("github"),
    APPLE("apple"),
    MICROSOFT("microsoft");

    private final String dbValue;

    AuthProvider(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AuthProvider fromDbValue(String dbValue) {
        return Arrays.stream(values())
                .filter(v -> v.dbValue.equals(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AuthProvider db value: " + dbValue));
    }
}
