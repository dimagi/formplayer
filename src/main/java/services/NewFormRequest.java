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

    private NewFormRequest(String formUrl, Map<String, String> authDict, String username, String domain, String lang,
                           Map<String, String> sessionData, String instanceContent, String postUrl,
                           FormSessionRepo formSessionRepo, XFormService xFormService,
                           RestoreService restoreService, String authToken) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formUrl = formUrl;
        this.auth = getAuth(authDict, authToken);
        this.domain = domain;
        try {
            formEntrySession = new FormSession(getFormXml(), getRestoreXml(),
                    lang, username, domain, sessionData, postUrl, instanceContent);
            formSessionRepo.save(formEntrySession.serialize());

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public NewFormRequest(SerializableFormSession session, RestoreService restoreService, String authToken) throws Exception {
        this.restoreService = restoreService;
        this.auth = getAuth(null, authToken);
        this.domain = session.getDomain();
        session.setRestoreXml(getRestoreXml());
        formEntrySession = new FormSession(session);
    }

    //TODO WSP combine this with logic in MenuController
    private HqAuth getAuth(Map<String, String> authMap, String authToken){
        HqAuth auth = null;
        if(authToken != null && !authToken.trim().equals("")){
            auth = new DjangoAuth(authToken);
        } else if(authMap.containsKey("type")){
            if(authMap.get("type").equals("django-session")){
                auth = new DjangoAuth(authMap.get("key"));
            }
        }
        return auth;
    }

    public NewFormRequest(NewSessionRequestBean bean, String postUrl, FormSessionRepo formSessionRepo,
                          XFormService xFormService, RestoreService restoreService, String authToken) throws Exception {
        this(bean.getFormUrl(),
                bean.getHqAuth(),
                bean.getSessionData().getUsername(),
                bean.getSessionData().getDomain(),
                bean.getLang(),
                bean.getSessionData().getData(),
                bean.getInstanceContent(),
                postUrl,
                formSessionRepo,
                xFormService,
                restoreService,
                authToken);
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
