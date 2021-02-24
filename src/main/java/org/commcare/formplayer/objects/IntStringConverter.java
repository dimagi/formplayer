package org.commcare.formplayer.objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class IntStringConverter implements AttributeConverter<Integer, String> {

    @Override
    public String convertToDatabaseColumn(Integer attribute) {
        return Integer.toString(attribute);
    }

    @Override
    public Integer convertToEntityAttribute(String dbData) {
        return Integer.parseInt(dbData);
    }
}
