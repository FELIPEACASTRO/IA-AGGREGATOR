package com.ia.aggregator.domain.auth.vo;

import java.util.Arrays;

/**
 * User roles in the platform.
 */
public enum UserRole {
    SUPER_ADMIN("super_admin"),
    ADMIN("admin"),
    USER("user"),
    VIEWER("viewer"),
    API_ONLY("api_only");

    private final String dbValue;

    UserRole(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static UserRole fromDbValue(String dbValue) {
        return Arrays.stream(values())
                .filter(v -> v.dbValue.equals(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown UserRole db value: " + dbValue));
    }
}
