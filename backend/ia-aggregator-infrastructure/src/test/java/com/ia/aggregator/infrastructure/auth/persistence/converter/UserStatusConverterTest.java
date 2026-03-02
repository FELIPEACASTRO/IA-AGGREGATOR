package com.ia.aggregator.infrastructure.auth.persistence.converter;

import com.ia.aggregator.domain.auth.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusConverterTest {

    private final UserStatusConverter converter = new UserStatusConverter();

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void convertToDatabaseColumn_shouldReturnDbValue(UserStatus status) {
        String dbValue = converter.convertToDatabaseColumn(status);
        assertEquals(status.getDbValue(), dbValue);
    }

    @Test
    void convertToDatabaseColumn_shouldReturnNullForNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void convertToEntityAttribute_shouldReturnEnum(UserStatus status) {
        UserStatus result = converter.convertToEntityAttribute(status.getDbValue());
        assertEquals(status, result);
    }

    @Test
    void convertToEntityAttribute_shouldReturnNullForNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
