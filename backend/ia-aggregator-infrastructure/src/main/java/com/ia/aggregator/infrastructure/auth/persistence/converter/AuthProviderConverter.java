package com.ia.aggregator.infrastructure.auth.persistence.converter;

import com.ia.aggregator.domain.auth.vo.AuthProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AuthProviderConverter implements AttributeConverter<AuthProvider, String> {

    @Override
    public String convertToDatabaseColumn(AuthProvider attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public AuthProvider convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AuthProvider.fromDbValue(dbData);
    }
}
