package services;

import beans.NewFormResponse;
import beans.NewSessionRequestBean;
import objects.SerializableFormSession;
import org.apache.commons.io.IOUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.xform.util.XFormUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import sandbox.UserSqlSandbox;
import session.FormSession;
import util.Constants;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class NewFormResponseFactory {

    @Autowired
    private XFormService xFormService;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private FormSessionRepo formSessionRepo;

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
        }
        else if (bean.getFormContent() != null) {
            formXml = bean.getFormContent();
        } else {
            throw new RuntimeException("No FormURL or FormContent");
        }
        // Don't purge when restoring as a case
        boolean shouldPurge = bean.getRestoreAsCaseId() == null;
        UserSqlSandbox sandbox = restoreFactory.performTimedSync(shouldPurge);

        storageFactory.configure(bean.getUsername(), bean.getDomain(), bean.getSessionData().getAppId(), bean.getRestoreAs());

        FormSession formSession = new FormSession(
                sandbox,
                parseFormDef(formXml),
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
                bean.getRestoreAsCaseId()
        );


        formSessionRepo.save(formSession.serialize());
        NewFormResponse response = new NewFormResponse(formSession);

        if (bean.getNavMode() != null && bean.getNavMode().equals(Constants.NAV_MODE_PROMPT)) {
            response.setEvent(response.getTree()[0]);
            response.setTree(null);
        }
        return response;
    }

    public NewFormResponse getResponse(SerializableFormSession session) throws Exception {
        FormSession formSession = getFormSession(session);
        return new NewFormResponse(formSession);
    }

    public FormSession getFormSession(SerializableFormSession serializableFormSession) throws Exception {
        return new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler, storageFactory);
    }

    private String getFormXml(String formUrl) {
        return xFormService.getFormXml(formUrl);
    }

    private static FormDef parseFormDef(String formXml) throws IOException {
        FormDef formDef = XFormUtils.getFormRaw(new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
        return formDef;
    }
}
