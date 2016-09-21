package services;

import auth.HqAuth;
import beans.NewFormResponse;
import beans.NewSessionRequestBean;
import hq.CaseAPIs;
import objects.SerializableFormSession;
import org.commcare.api.persistence.UserSqlSandbox;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.XFormParser;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import session.FormSession;

import java.io.IOException;
import java.io.StringReader;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class NewFormResponseFactory {

    private final XFormService xFormService;
    private final RestoreFactory restoreFactory;
    private final FormSessionRepo formSessionRepo;

    public NewFormResponseFactory(FormSessionRepo formSessionRepo,
                                  XFormService xFormService,
                                  RestoreFactory restoreFactory) {
        this.xFormService = xFormService;
        this.restoreFactory = restoreFactory;
        this.formSessionRepo = formSessionRepo;
    }

    public NewFormResponse getResponse(NewSessionRequestBean bean, String postUrl, HqAuth auth) throws Exception {

        String formXml = getFormXml(bean.getFormUrl(), auth);
        UserSqlSandbox sandbox = CaseAPIs.restoreIfNotExists(restoreFactory.getUsername(),
                restoreFactory,
                restoreFactory.getDomain());

        FormSession formSession = new FormSession(sandbox, parseFormDef(formXml), bean.getSessionData().getUsername(),
                bean.getSessionData().getDomain(), bean.getSessionData().getData(), postUrl, bean.getLang(), null,
                bean.getInstanceContent(), bean.getOneQuestionPerScreen(), bean.getAsUser());

        formSessionRepo.save(formSession.serialize());
        return new NewFormResponse(formSession);
    }

    public NewFormResponse getResponse(SerializableFormSession session)
            throws Exception {
        FormSession formSession = new FormSession(session);
        return new NewFormResponse(formSession);
    }

    private String getFormXml(String formUrl, HqAuth auth) {
        return xFormService.getFormXml(formUrl, auth);
    }

    private FormDef parseFormDef(String formXml) throws IOException {
        XFormParser mParser = new XFormParser(new StringReader(formXml));
        return mParser.parse();
    }
}
