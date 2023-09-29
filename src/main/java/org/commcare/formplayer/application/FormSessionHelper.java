package org.commcare.formplayer.application;

import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormDefinitionService;
import org.commcare.formplayer.services.FormplayerRemoteInstanceFetcher;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.MenuSessionRunnerService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.VirtualDataInstanceService;
import org.commcare.formplayer.session.FormSession;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class FormSessionHelper {

    @Autowired
    protected MenuSessionRunnerService runnerService;

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    private VirtualDataInstanceService virtualDataInstanceService;

    @Autowired
    protected FormSendCalloutHandler formSendCalloutHandler;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    private FormDefinitionService formDefinitionService;

    public FormSession getFormSession(SerializableFormSession serializableFormSession) throws Exception {
        CommCareSession commCareSession = getCommCareSession(serializableFormSession.getMenuSessionId());
        return getFormSession(serializableFormSession, commCareSession);
    }

    @NotNull
    public FormSession getFormSession(SerializableFormSession serializableFormSession,
            @Nullable CommCareSession commCareSession) throws Exception {
        FormplayerRemoteInstanceFetcher formplayerRemoteInstanceFetcher = new FormplayerRemoteInstanceFetcher(
                runnerService.getCaseSearchHelper(),
                virtualDataInstanceService);
        return new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory,
                commCareSession,
                formplayerRemoteInstanceFetcher,
                formDefinitionService
        );
    }
}
