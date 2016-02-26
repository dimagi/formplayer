package requests;

import auth.DjangoAuth;
import auth.HqAuth;
import beans.NewSessionRequestBean;
import beans.NewFormSessionResponse;
import org.springframework.stereotype.Service;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import session.FormEntrySession;

import java.io.IOException;
import java.util.Map;

/**
 * Created by willpride on 1/14/16.
 */
@Service
public class NewFormRequest {

    String formUrl;
    FormEntrySession formEntrySession;
    SessionRepo sessionRepo;
    XFormService xFormService;
    RestoreService restoreService;
    HqAuth auth;
    String username;
    String domain;
    String lang;

    public NewFormRequest(String formUrl, Map<String, String> authDict, String username, String domain, String lang,
                          Map<String, String> sessionData, SessionRepo sessionRepo,
                          XFormService xFormService, RestoreService restoreService) throws Exception {
        this.sessionRepo = sessionRepo;
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formUrl = formUrl;
        this.auth = getAuth((authDict));
        this.username = username;
        this.domain = domain;
        this.lang = lang;
        Map<String, String> data = sessionData;
        try {
            formEntrySession = new FormEntrySession(getFormXml(), getRestoreXml(), lang, username, data);
            sessionRepo.save(formEntrySession.serialize());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public HqAuth getAuth(Map<String, String> authMap){
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
        NewFormSessionResponse ret = new NewFormSessionResponse(formEntrySession);
        return ret;
    }

    public String getRestoreXml(){
        String restorePayload = restoreService.getRestoreXml(domain, auth);
        return restorePayload;
    }

    public String getFormXml(){
        return xFormService.getFormXml(formUrl, auth);
    }
}
