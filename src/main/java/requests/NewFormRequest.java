package requests;

import auth.DjangoAuth;
import auth.HqAuth;
import beans.NewSessionRequestBean;
import beans.NewFormSessionResponse;
import org.springframework.stereotype.Service;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import session.FormSession;

import java.io.IOException;
import java.util.Map;

/**
 * Created by willpride on 1/14/16.
 */
@Service
public class NewFormRequest {

    private final String formUrl;
    private FormSession formEntrySession;
    private final XFormService xFormService;
    private final RestoreService restoreService;
    private final HqAuth auth;
    private final String domain;

    private NewFormRequest(String formUrl, Map<String, String> authDict, String username, String domain, String lang,
                           Map<String, String> sessionData, SessionRepo sessionRepo,
                           XFormService xFormService, RestoreService restoreService, String authToken) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formUrl = formUrl;
        this.auth = getAuth(authDict, authToken);
        this.domain = domain;
        try {
            formEntrySession = new FormSession(getFormXml(), getRestoreXml(), lang, username, domain, sessionData);
            sessionRepo.save(formEntrySession.serialize());
        } catch(IOException e){
            e.printStackTrace();
        }
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

    public NewFormRequest(NewSessionRequestBean bean, SessionRepo sessionRepo,
                          XFormService xFormService, RestoreService restoreService, String authToken) throws Exception {
        this(bean.getFormUrl(), bean.getHqAuth(),
                bean.getSessionData().getUsername(), bean.getSessionData().getDomain(),
                bean.getLang(), bean.getSessionData().getData(), sessionRepo, xFormService, restoreService, authToken);
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
