package services;

import auth.DjangoAuth;
import auth.HqAuth;
import beans.NewFormSessionResponse;
import beans.NewSessionRequestBean;
import objects.SerializableFormSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import session.FormSession;

import java.io.IOException;
import java.util.Map;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 * TODO WSP this should probably just be a static helper class, or merged with FormSession
 */
@Component
@EnableAutoConfiguration
public class NewFormRequest {

    private String formUrl;
    private FormSession formEntrySession;
    private XFormService xFormService;
    private final RestoreService restoreService;
    private final HqAuth auth;
    private String domain;

    public NewFormRequest(NewSessionRequestBean bean, String postUrl, FormSessionRepo formSessionRepo,
                          XFormService xFormService, RestoreService restoreService, HqAuth auth) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formUrl = bean.getFormUrl();
        this.auth = auth;
        this.domain = bean.getSessionData().getDomain();
        try {
            formEntrySession = new FormSession(getFormXml(), getRestoreXml(),
                    bean.getLang(), bean.getSessionData().getUsername(), domain, bean.getSessionData().getData(),
                    postUrl, bean.getInstanceContent(), bean.getOneQuestionPerScreen());
            formSessionRepo.save(formEntrySession.serialize());

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public NewFormRequest(SerializableFormSession session, RestoreService restoreService, HqAuth auth) throws Exception {
        this.restoreService = restoreService;
        this.auth = auth;
        this.domain = session.getDomain();
        session.setRestoreXml(getRestoreXml());
        formEntrySession = new FormSession(session);
    }

    public NewFormSessionResponse getResponse() throws IOException {
        return new NewFormSessionResponse(formEntrySession);
    }

    private String getRestoreXml(){
        return restoreService.getRestoreXml(domain, auth);
    }

    private String getFormXml(){
        return xFormService.getFormXml(formUrl, auth);
    }
}
