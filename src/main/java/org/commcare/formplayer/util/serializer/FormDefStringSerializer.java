package org.commcare.formplayer.util.serializer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.externalizable.DeserializationException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FormDefStringSerializer {

    public static String serialize(FormDef formDef) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream serializedStream = new DataOutputStream(baos);
        formDef.writeExternal(serializedStream);
        return Base64.encodeBase64String(baos.toByteArray());
    }

    public static FormDef deserialize(String serializedFormDef)
            throws IOException, DeserializationException {
        byte[] sessionBytes = Base64.decodeBase64(serializedFormDef);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sessionBytes));
        FormDef formDef = new FormDef();
        formDef.readExternal(inputStream, PrototypeManager.getDefault());
        return formDef;
    }

}
