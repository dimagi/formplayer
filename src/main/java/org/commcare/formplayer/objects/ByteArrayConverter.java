package org.commcare.formplayer.objects;

import org.springframework.util.SerializationUtils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ByteArrayConverter implements AttributeConverter<Object, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(Object attribute) {
        return SerializationUtils.serialize(attribute);
    }

    @Override
    public Object convertToEntityAttribute(byte[] dbData) {
        return SerializationUtils.deserialize(dbData);
    }
}
