package com.ia.aggregator.domain.auth.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING_VERIFICATION, pending_verification",
            "ACTIVE, active",
            "INACTIVE, inactive",
            "SUSPENDED, suspended",
            "DELETED, deleted"
    })
    void getDbValue_shouldReturnCorrectMapping(UserStatus status, String expectedDbValue) {
        assertEquals(expectedDbValue, status.getDbValue());
    }

    @ParameterizedTest
    @CsvSource({
            "pending_verification, PENDING_VERIFICATION",
            "active, ACTIVE",
            "inactive, INACTIVE",
            "suspended, SUSPENDED",
            "deleted, DELETED"
    })
    void fromDbValue_shouldReturnCorrectEnum(String dbValue, UserStatus expectedStatus) {
        assertEquals(expectedStatus, UserStatus.fromDbValue(dbValue));
    }

    @Test
    void fromDbValue_shouldThrowForUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromDbValue("unknown"));
    }
}
