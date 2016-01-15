package requests;

import application.NewFormResponse;
import org.commcare.api.json.WalkJson;
import org.javarosa.core.model.FormDef;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import session.FormEntrySession;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

/**
 * Created by willpride on 1/14/16.
 */
public class NewFormRequest extends AuthRequest {

    String formUrl;
    String[] langs;
    String title;
    FormEntrySession formEntrySession;

    public NewFormRequest(String body) throws IOException {
        super(body);
        JSONObject jsonBody = new JSONObject(body);
        formUrl = jsonBody.getString("form-url");
        String initLang = jsonBody.getString("lang");
        formEntrySession = new FormEntrySession(getFormXml(), initLang);
    }

    public NewFormResponse getResponse() throws IOException {
        NewFormResponse ret = new NewFormResponse(formEntrySession);
        return ret;
    }

    private String getFormXml(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(formUrl,
                        HttpMethod.GET,
                        new HttpEntity<String>(getAuth().getAuthHeaders()), String.class);
        return response.getBody();
    }
}
