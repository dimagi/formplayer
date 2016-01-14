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

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

/**
 * Created by willpride on 1/14/16.
 */
public class NewFormRequest extends AuthRequest {

    String username;
    String domain;
    String formUrl;
    String initLang;

    String[] langs;
    String title;

    public NewFormRequest(String body) {
        super(body);
        JSONObject jsonBody = new JSONObject(body);
        JSONObject sessionData = jsonBody.getJSONObject("session-data");
        username = sessionData.getString("username");
        domain = sessionData.getString("domain");
        formUrl = jsonBody.getString("form-url");
        initLang = jsonBody.getString("lang");
    }

    public NewFormResponse getResponse() throws IOException {
        NewFormResponse ret = new NewFormResponse(getFormTree(), langs, title, UUID.randomUUID().toString());
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

    private JSONArray getFormTree() throws IOException {
        String formXml = getFormXml();
        FormDef formDef = parseFormDef(formXml);
        FormEntryModel fem = new FormEntryModel(formDef, FormEntryModel.REPEAT_STRUCTURE_LINEAR);
        FormEntryController fec = new FormEntryController(fem);
        fec.setLanguage(initLang);
        title = formDef.getTitle();
        langs = fem.getLanguages();
        JSONArray ret = WalkJson.walkToJSON(fem, fec);
        return ret;
    }

    private FormDef parseFormDef(String formXml) throws IOException {
        XFormParser mParser = new XFormParser(new StringReader(formXml));
        return mParser.parse();
    }
}
