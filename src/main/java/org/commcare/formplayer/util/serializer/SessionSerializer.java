package org.commcare.formplayer.util.serializer;

import org.commcare.session.CommCareSession;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.util.externalizable.DeserializationException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SessionSerializer {
    public static byte[] serialize(CommCareSession session) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        DataOutputStream oos;
        try {
            oos = new DataOutputStream(baos);
            session.serializeSessionState(oos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    public static CommCareSession deserialize(CommCarePlatform platform, byte[] bytes) throws DeserializationException, IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        return CommCareSession.restoreSessionFromStream(platform, in);
    }
}
