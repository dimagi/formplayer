package org.commcare.formplayer.application;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormDefinitionService;
import org.commcare.formplayer.services.FormplayerRemoteInstanceFetcher;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.MenuSessionRunnerService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.VirtualDataInstanceService;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.modern.database.TableBuilder;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class FormSessionFactory {

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

    @Autowired
    private CommCareSessionFactory commCareSessionFactory;

    public FormSession getFormSession(SerializableFormSession serializableFormSession, String windowWidth) throws Exception {
        CommCareSession commCareSession = commCareSessionFactory.getCommCareSession(serializableFormSession.getMenuSessionId());
        return getFormSession(serializableFormSession, commCareSession, windowWidth);
    }

    @NotNull
    public FormSession getFormSession(SerializableFormSession serializableFormSession,
            @Nullable CommCareSession commCareSession, @Nullable String windowWidth) throws Exception {
        verifyPublicSessionOwnership(serializableFormSession);
        FormplayerRemoteInstanceFetcher formplayerRemoteInstanceFetcher = new FormplayerRemoteInstanceFetcher(
                runnerService.getCaseSearchHelper(),
                virtualDataInstanceService);
        return new FormSession(serializableFormSession,
                restoreFactory,
                formSendCalloutHandler,
                storageFactory,
                commCareSession,
                formplayerRemoteInstanceFetcher,
                formDefinitionService,
                windowWidth
        );
    }

    /**
     * A public web apps session may only operate on the form session that its own one-time link
     * created. The session stores the scrubbed authoritative username, so any session bound to a
     * different user/domain is rejected. No-op for non-public sessions and non-request contexts.
     *
     * Package-private for testing.
     */
    void verifyPublicSessionOwnership(SerializableFormSession session) {
        Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
        if (userDetails.isEmpty() || !userDetails.get().isPublicSession()) {
            return;
        }
        HqUserDetailsBean details = userDetails.get();
        boolean owned = Objects.equals(session.getDomain(), details.getDomain())
                && Objects.equals(session.getUsername(), TableBuilder.scrubName(details.getUsername()));
        if (!owned) {
            throw new FormNotFoundException(session.getId());
        }
    }
}
