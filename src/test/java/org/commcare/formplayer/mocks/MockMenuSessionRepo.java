package org.commcare.formplayer.mocks;

import org.commcare.formplayer.repo.MenuSessionRepo;
import org.commcare.formplayer.repo.SerializableMenuSession;

/**
 * Created by willpride on 12/7/16.
 */
public abstract class MockMenuSessionRepo implements MenuSessionRepo {
    private final SerializableMenuSession serializableMenuSession = new SerializableMenuSession();

    @Override
    public SerializableMenuSession findOneWrapped(String id) {
        return serializableMenuSession;
    }

    @Override
    public SerializableMenuSession save(SerializableMenuSession toBeSaved) {
        serializableMenuSession.setId(toBeSaved.getId());
        serializableMenuSession.setCommcareSession(toBeSaved.getCommcareSession());
        serializableMenuSession.setUsername(toBeSaved.getUsername());
        serializableMenuSession.setDomain(toBeSaved.getDomain());
        serializableMenuSession.setAppId(toBeSaved.getAppId());
        serializableMenuSession.setInstallReference(toBeSaved.getInstallReference());
        serializableMenuSession.setLocale(toBeSaved.getLocale());
        return serializableMenuSession;
    }


}
