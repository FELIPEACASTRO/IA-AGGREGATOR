package com.ia.aggregator.infrastructure.auth.persistence.converter;

import com.ia.aggregator.domain.auth.vo.AuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthProviderConverterTest {

    private final AuthProviderConverter converter = new AuthProviderConverter();

    @ParameterizedTest
    @EnumSource(AuthProvider.class)
    void convertToDatabaseColumn_shouldReturnDbValue(AuthProvider provider) {
        String dbValue = converter.convertToDatabaseColumn(provider);
        assertEquals(provider.getDbValue(), dbValue);
    }

    @Test
    void convertToDatabaseColumn_shouldReturnNullForNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @ParameterizedTest
    @EnumSource(AuthProvider.class)
    void convertToEntityAttribute_shouldReturnEnum(AuthProvider provider) {
        AuthProvider result = converter.convertToEntityAttribute(provider.getDbValue());
        assertEquals(provider, result);
    }

    @Test
    void convertToEntityAttribute_shouldReturnNullForNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
