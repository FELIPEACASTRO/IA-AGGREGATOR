package com.ia.aggregator.domain.auth.vo;

import java.util.Arrays;

/**
 * User status in the system lifecycle.
 */
public enum UserStatus {
    PENDING_VERIFICATION("pending_verification"),
    ACTIVE("active"),
    INACTIVE("inactive"),
    SUSPENDED("suspended"),
    DELETED("deleted");

    private final String dbValue;

    UserStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static UserStatus fromDbValue(String dbValue) {
        return Arrays.stream(values())
                .filter(v -> v.dbValue.equals(dbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown UserStatus db value: " + dbValue));
    }
}
