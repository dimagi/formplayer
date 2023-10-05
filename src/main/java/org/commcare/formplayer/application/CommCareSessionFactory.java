package org.commcare.formplayer.application;

import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.MenuSessionService;
import org.commcare.formplayer.util.serializer.SessionSerializer;
import org.commcare.session.CommCareSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CommCareSessionFactory {

    @Autowired
    protected MenuSessionService menuSessionService;

    @Autowired
    protected InstallService installService;

    @Nullable
    public CommCareSession getCommCareSession(String menuSessionId) throws Exception {
        if (menuSessionId == null || menuSessionId.trim().equals("")) {
            return null;
        }

        SerializableMenuSession serializableMenuSession = menuSessionService.getSessionById(menuSessionId);
        FormplayerConfigEngine engine = installService.configureApplication(
                serializableMenuSession.getInstallReference(),
                serializableMenuSession.isPreview()).first;
        return SessionSerializer.deserialize(engine.getPlatform(), serializableMenuSession.getCommcareSession());
    }
}
