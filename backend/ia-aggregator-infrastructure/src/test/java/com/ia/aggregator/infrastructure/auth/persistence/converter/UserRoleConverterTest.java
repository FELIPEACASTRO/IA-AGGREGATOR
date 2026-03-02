package com.ia.aggregator.infrastructure.auth.persistence.converter;

import com.ia.aggregator.domain.auth.vo.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleConverterTest {

    private final UserRoleConverter converter = new UserRoleConverter();

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void convertToDatabaseColumn_shouldReturnDbValue(UserRole role) {
        String dbValue = converter.convertToDatabaseColumn(role);
        assertEquals(role.getDbValue(), dbValue);
    }

    @Test
    void convertToDatabaseColumn_shouldReturnNullForNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void convertToEntityAttribute_shouldReturnEnum(UserRole role) {
        UserRole result = converter.convertToEntityAttribute(role.getDbValue());
        assertEquals(role, result);
    }

    @Test
    void convertToEntityAttribute_shouldReturnNullForNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void roundTrip_shouldPreserveValue() {
        for (UserRole role : UserRole.values()) {
            String db = converter.convertToDatabaseColumn(role);
            UserRole back = converter.convertToEntityAttribute(db);
            assertEquals(role, back);
        }
    }
}
