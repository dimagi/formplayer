package requests;

import auth.DjangoAuth;
import auth.HqAuth;
import beans.NewSessionRequestBean;
import beans.NewFormSessionResponse;
import objects.SerializableFormSession;
import org.springframework.stereotype.Service;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import session.FormSession;

import java.io.IOException;
import java.util.Map;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 * TODO WSP this should probably just be a static helper class, or merged with FormSession
 */
@Service
public class NewFormRequest {

    private String formUrl;
    private FormSession formEntrySession;
    private XFormService xFormService;
    private final RestoreService restoreService;
    private final HqAuth auth;
    private String domain;

    public NewFormRequest(NewSessionRequestBean bean, SessionRepo sessionRepo,
                          XFormService xFormService, RestoreService restoreService, String authToken) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formUrl = bean.getFormUrl();
        this.auth = getAuth(bean.getHqAuth(), authToken);
        this.domain = bean.getSessionData().getDomain();
        try {
            formEntrySession = new FormSession(getFormXml(), getRestoreXml(),
                    bean.getLang(), bean.getSessionData().getUsername(),
                    domain, bean.getSessionData().getData(), bean.getInstanceContent());
            sessionRepo.save(formEntrySession.serialize());
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
