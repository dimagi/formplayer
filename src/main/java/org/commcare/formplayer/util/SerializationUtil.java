package org.commcare.formplayer.util;

import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class SerializationUtil {

    public static <T extends Externalizable> T deserialize(byte[] bytes, Class<T> type) {
        T t;
        try {
            t = type.newInstance();
            t.readExternal(new DataInputStream(new ByteArrayInputStream(bytes)), PrototypeManager.getDefault());
        } catch (IllegalAccessException e) {
            throw logAndWrap(e, type, "Illegal Access Exception");
        } catch (InstantiationException e) {
            throw logAndWrap(e, type, "Instantiation Exception");
        } catch (IOException e) {
            throw logAndWrap(e, type, "Totally non-sensical IO Exception");
        } catch (DeserializationException e) {
            throw logAndWrap(e, type, "CommCare ran into an issue deserializing data");
        }
        return t;
    }

    private static RuntimeException logAndWrap(Exception e, Class type, String message) {
        RuntimeException re = new RuntimeException(message + " while inflating type " + type.getName());
        re.initCause(e);
        Logger.log("Error:", e.getMessage());
        return re;
    }
}
