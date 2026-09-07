package com.example.VegeTabell.app.entity.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NotificationType.fromValue(dbData);
    }
}
