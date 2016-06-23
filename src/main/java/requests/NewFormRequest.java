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
