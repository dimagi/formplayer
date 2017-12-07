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
import repo.impl.PostgresFormSessionRepo;
import sandbox.UserSqlSandbox;
import session.FormSession;

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

    public NewFormResponse getResponse(NewSessionRequestBean bean, String postUrl) throws Exception {

        String formXml = getFormXml(bean.getFormUrl());
        UserSqlSandbox sandbox = restoreFactory.performTimedSync();

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
                formSendCalloutHandler
        );

        formSessionRepo.save(formSession.serialize());
        return new NewFormResponse(formSession);
    }

    public NewFormResponse getResponse(SerializableFormSession session) throws Exception {
        FormSession formSession = getFormSession(session);
        return new NewFormResponse(formSession);
    }

    public FormSession getFormSession(SerializableFormSession serializableFormSession) throws Exception {
        return new FormSession(serializableFormSession, restoreFactory, formSendCalloutHandler);
    }

    private String getFormXml(String formUrl) {
        return xFormService.getFormXml(formUrl);
    }

    private static FormDef parseFormDef(String formXml) throws IOException {
        FormDef formDef = XFormUtils.getFormRaw(new InputStreamReader(IOUtils.toInputStream(formXml, "UTF-8")));
        return formDef;
    }
}
