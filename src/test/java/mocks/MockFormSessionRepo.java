package mocks;

import objects.SerializableFormSession;
import repo.FormSessionRepo;

/**
 * Created by willpride on 12/7/16.
 */
public abstract class MockFormSessionRepo implements FormSessionRepo {
    private final SerializableFormSession serializableFormSession = new SerializableFormSession();

    @Override
    public SerializableFormSession findOneWrapped(String id) {
        return serializableFormSession;
    }

    @Override
    public SerializableFormSession save(SerializableFormSession toBeSaved) {
        serializableFormSession.setInstanceXml(toBeSaved.getInstanceXml());
        serializableFormSession.setFormXml(toBeSaved.getFormXml());
        serializableFormSession.setUsername(toBeSaved.getUsername());
        serializableFormSession.setSessionData(toBeSaved.getSessionData());
        serializableFormSession.setDomain(toBeSaved.getDomain());
        serializableFormSession.setMenuSessionId(toBeSaved.getMenuSessionId());
        serializableFormSession.setAppId(toBeSaved.getAppId());
        serializableFormSession.setOneQuestionPerScreen(toBeSaved.getOneQuestionPerScreen());
        serializableFormSession.setCurrentIndex(toBeSaved.getCurrentIndex());
        return serializableFormSession;
    }


}
