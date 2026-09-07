package com.example.VegeTabell.app.entity.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CanceledByConverter implements AttributeConverter<CanceledBy, String> {

    @Override
    public String convertToDatabaseColumn(CanceledBy attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CanceledBy convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CanceledBy.fromValue(dbData);
    }
}
