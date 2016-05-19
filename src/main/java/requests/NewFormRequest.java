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

    private String formUrl;
    private FormSession formEntrySession;
    private XFormService xFormService;
    private RestoreService restoreService;
    private HqAuth auth;
    private String domain;

    private NewFormRequest(String formUrl, Map<String, String> authDict, String username, String domain, String lang,
                           Map<String, String> sessionData, SessionRepo sessionRepo,
                           XFormService xFormService, RestoreService restoreService) throws Exception {
        SessionRepo sessionRepo1 = sessionRepo;
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formUrl = formUrl;
        this.auth = getAuth((authDict));
        String username1 = username;
        this.domain = domain;
        String lang1 = lang;
        try {
            formEntrySession = new FormSession(getFormXml(), getRestoreXml(), lang, username, domain, sessionData);
            sessionRepo.save(formEntrySession.serialize());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private HqAuth getAuth(Map<String, String> authMap){
        if(authMap.containsKey("type")){
            if(authMap.get("type").equals("django-session")){
                return new DjangoAuth(authMap.get("key"));
            }
        }
        return null;
    }

    public NewFormRequest(NewSessionRequestBean bean, SessionRepo sessionRepo,
                          XFormService xFormService, RestoreService restoreService) throws Exception {
        this(bean.getFormUrl(), bean.getHqAuth(),
                bean.getSessionData().getUsername(), bean.getSessionData().getDomain(),
                bean.getLang(), bean.getSessionData().getData(), sessionRepo, xFormService, restoreService);
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
