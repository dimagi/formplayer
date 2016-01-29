package requests;

import beans.NewSessionResponse;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.NewSessionRequestBean;
import objects.SerializableSession;
import org.springframework.stereotype.Service;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import session.FormEntrySession;

import java.io.IOException;

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
    String restoreXml;

    public NewFormRequest(NewSessionRequestBean bean, SessionRepo sessionRepo,
                          XFormService xFormService, RestoreService restoreService) throws Exception {
        this.sessionRepo = sessionRepo;
        this.xFormService = xFormService;
        this.restoreService = restoreService;

        formUrl = bean.getFormUrl();
        auth = new DjangoAuth(bean.getHqAuth().get("django-session"));
        username = bean.getSessionData().getUsername();
        domain = bean.getSessionData().getDomain();
        String initLang = bean.getLang();
        try {
            formEntrySession = new FormEntrySession(getFormXml(), getRestoreXml(), initLang, username);
            sessionRepo.save(serialize());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public NewSessionResponse getResponse() throws IOException {
        NewSessionResponse ret = new NewSessionResponse(formEntrySession);
        return ret;
    }

    public String getRestoreXml(){
        return restoreService.getRestoreXml(domain, auth);
    }

    public String getFormXml(){
        return xFormService.getFormXml(formUrl, auth);
    }

    private SerializableSession serialize() throws IOException {
        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setInstanceXml(formEntrySession.getInstanceXml());
        serializableSession.setId(formEntrySession.getUUID());
        serializableSession.setFormXml(formEntrySession.getFormXml());
        serializableSession.setUsername(username);
        serializableSession.setRestoreXml(formEntrySession.getRestoreXml());
        serializableSession.setSequenceId(formEntrySession.getSequenceId());
        serializableSession.setInitLang(formEntrySession.getInitLang());
        return serializableSession;
    }
}
