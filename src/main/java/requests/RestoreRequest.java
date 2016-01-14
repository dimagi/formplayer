package requests;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import hq.HttpUtils;
import hq.RestoreUtils;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Created by willpride on 1/12/16.
 */
public class RestoreRequest extends AuthRequest{
    String username;
    String domain;
    final static String host = "localhost:8000";
    String password;

    public RestoreRequest(String body){
        super(body);
        JSONObject jsonBody = new JSONObject(body);
        JSONObject sessionData = getSessionData(jsonBody);
        username = sessionData.getString("username");
        domain = sessionData.getString("domain");
    }

    public RestoreRequest(String username, String password, String domain, String host){
        super(username, password);
        this.username = username;
        this.password = password;
        this.domain = domain;
    }

    public String getHost() {
        return host;
    }

    public String getUsername(){
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getRestorePayload() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange("http://" + getHost()
                                + "/a/" + getDomain() + "/phone/restore/?version=2.0",
                        HttpMethod.GET,
                        new HttpEntity<String>(getAuth().getAuthHeaders()), String.class);
        return response.getBody();
    }
}
