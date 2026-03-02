package com.ia.aggregator.domain.auth.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthProviderTest {

    @ParameterizedTest
    @CsvSource({
            "LOCAL, email",
            "GOOGLE, google",
            "GITHUB, github",
            "APPLE, apple",
            "MICROSOFT, microsoft"
    })
    void getDbValue_shouldReturnCorrectMapping(AuthProvider provider, String expectedDbValue) {
        assertEquals(expectedDbValue, provider.getDbValue());
    }

    @ParameterizedTest
    @CsvSource({
            "email, LOCAL",
            "google, GOOGLE",
            "github, GITHUB",
            "apple, APPLE",
            "microsoft, MICROSOFT"
    })
    void fromDbValue_shouldReturnCorrectEnum(String dbValue, AuthProvider expectedProvider) {
        assertEquals(expectedProvider, AuthProvider.fromDbValue(dbValue));
    }

    @Test
    void fromDbValue_shouldThrowForUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> AuthProvider.fromDbValue("unknown"));
    }
}
