package org.commcare.formplayer.services;

import org.apache.commons.io.IOUtils;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.NewSessionRequestBean;
import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.core.services.locale.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class NewFormResponseFactory {

    @Autowired
    private WebClient webClient;

    @Autowired
    private CaseSearchHelper caseSearchHelper;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private FormSessionService formSessionService;

    @Autowired
    private FormDefinitionService formDefinitionService;

    @Autowired
    private FormSendCalloutHandler formSendCalloutHandler;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    public NewFormResponse getResponse(NewSessionRequestBean bean, String postUrl) throws Exception {

        String formXml = null;

        if (bean.getFormUrl() != null) {
            formXml = getFormXml(bean.getFormUrl());
        } else if (bean.getFormContent() != null) {
            formXml = bean.getFormContent();
        } else {
            throw new RuntimeException("No FormURL or FormContent");
        }
        // Don't purge when restoring as a case
        boolean shouldPurge = bean.getRestoreAsCaseId() == null;
        UserSqlSandbox sandbox = restoreFactory.performTimedSync(shouldPurge, false, false);

        storageFactory.configure(bean.getUsername(),
                bean.getDomain(),
                bean.getSessionData().getAppId(),
                bean.getRestoreAs(),
                bean.getRestoreAsCaseId());

        FormDef formDef = parseFormDef(formXml);
        SerializableFormDefinition serializableFormDefinition = this.formDefinitionService.getOrCreateFormDefinition(
                bean.getSessionData().getAppId(),
                formDef.getMainInstance().schema,
                formDef.getMainInstance().formVersion,
                formDef
        );
        FormSession formSession = new FormSession(
                sandbox,
                serializableFormDefinition,
                bean.getUsername(),
                bean.getDomain(),
                bean.getSessionData().getData(),
                postUrl,
                bean.getLang(),
                null,
                bean.getInstanceContent(),
                bean.getOneQuestionPerScreen(),
                bean.getRestoreAs(),
                bean.getSessionData().getAppId(),
                bean.getSessionData().getFunctionContext(),
                formSendCalloutHandler,
                storageFactory,
                Constants.NAV_MODE_PROMPT.equals(bean.getNavMode()),
                bean.getRestoreAsCaseId(),
                null,
                caseSearchHelper
        );

        NewFormResponse response = getResponse(formSession);
        if (bean.getNavMode() != null && bean.getNavMode().equals(Constants.NAV_MODE_PROMPT)) {
            response.setEvent(response.getTree()[0]);
            response.setTree(null);
        }
        return response;
    }

    public NewFormResponse getResponse(FormSession formEntrySession) throws Exception {
        // Calling getFormTree has side effects and must be done before the instanceXML is serialized
        String formTreeJson = formEntrySession.getFormTree().toString();

        SerializableFormSession serializedSession = formEntrySession.serialize();
        formSessionService.saveSession(serializedSession);
        NewFormResponse response = new NewFormResponse(
                formTreeJson, formEntrySession.getLanguages(), serializedSession.getTitle(),
                serializedSession.getId(), serializedSession.getVersion(),
                serializedSession.getInstanceXml()
        );

        String[] translationKey = {"repeat.dialog.add.new"};
        for (String key : translationKey) {
            String translation = Localization.getWithDefault(key, null);
            if (translation != null) {
                response.addToTranslation(key, translation);
            }
        }

        return response;
    }

    public NewFormResponse getResponse(SerializableFormSession session, CommCareSession commCareSession) throws Exception {
        FormSession formSession = getFormSession(session, commCareSession);
        String formTreeJson = formSession.getFormTree().toString();
        return new NewFormResponse(
                formTreeJson, formSession.getLanguages(), session.getTitle(),
                session.getId(), session.getVersion(),
                session.getInstanceXml()
        );
    }

    public FormSession getFormSession(SerializableFormSession serializableFormSession, CommCareSession commCareSession) throws Exception {
        return new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler, storageFactory, commCareSession, caseSearchHelper);
    }

    private String getFormXml(String formUrl) {
        return webClient.get(formUrl);
    }

    private static FormDef parseFormDef(String formXml) throws IOException {
        FormDef formDef = XFormUtils.getFormRaw(new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
        return formDef;
    }
}
