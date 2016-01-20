package requests;

import application.NewFormResponse;
import objects.SerializableSession;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import repo.SessionRepo;
import services.XFormService;
import session.FormEntrySession;

import java.io.IOException;

/**
 * Created by willpride on 1/14/16.
 */
@Service
public class NewFormRequest extends AuthRequest {

    String formUrl;
    FormEntrySession formEntrySession;
    SessionRepo sessionRepo;
    XFormService xFormService;

    public NewFormRequest(String body, SessionRepo sessionRepo, XFormService xFormService) throws IOException {
        super(body);
        this.sessionRepo = sessionRepo;
        this.xFormService = xFormService;
        JSONObject jsonBody = new JSONObject(body);
        formUrl = jsonBody.getString("form-url");
        String initLang = jsonBody.getString("lang");
        try {
            formEntrySession = new FormEntrySession(getFormXml(), initLang);
            sessionRepo.save(serialize());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public NewFormResponse getResponse() throws IOException {
        NewFormResponse ret = new NewFormResponse(formEntrySession);
        return ret;
    }

    public String getFormXml(){
        return xFormService.getFormXml(formUrl, auth);
    }

    private SerializableSession serialize() throws IOException {
        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setInstanceXml(formEntrySession.getFormXml());
        serializableSession.setId(formEntrySession.getUUID());
        return serializableSession;
    }
}
