package org.commcare.formplayer.objects;

import org.commcare.formplayer.util.SerializationUtil;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.externalizable.ExtUtil;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Serialization converter for {@link TreeElement}
 */
@Converter
public class TreeElementConverter implements AttributeConverter<TreeElement, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(TreeElement attribute) {
        return ExtUtil.serialize(attribute);
    }

    @Override
    public TreeElement convertToEntityAttribute(byte[] dbData) {
        return SerializationUtil.deserialize(dbData, TreeElement.class);
    }
}
