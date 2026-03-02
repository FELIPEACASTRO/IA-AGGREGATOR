package com.ia.aggregator.domain.auth.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {

    @ParameterizedTest
    @CsvSource({
            "SUPER_ADMIN, super_admin",
            "ADMIN, admin",
            "USER, user",
            "VIEWER, viewer",
            "API_ONLY, api_only"
    })
    void getDbValue_shouldReturnCorrectMapping(UserRole role, String expectedDbValue) {
        assertEquals(expectedDbValue, role.getDbValue());
    }

    @ParameterizedTest
    @CsvSource({
            "super_admin, SUPER_ADMIN",
            "admin, ADMIN",
            "user, USER",
            "viewer, VIEWER",
            "api_only, API_ONLY"
    })
    void fromDbValue_shouldReturnCorrectEnum(String dbValue, UserRole expectedRole) {
        assertEquals(expectedRole, UserRole.fromDbValue(dbValue));
    }

    @Test
    void fromDbValue_shouldThrowForUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> UserRole.fromDbValue("unknown"));
    }
}
