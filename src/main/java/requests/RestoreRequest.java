package requests;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import hq.RestoreUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Created by willpride on 1/12/16.
 */
public class RestoreRequest {
    String username;
    String domain;
    String host;
    HqAuth auth;

    public RestoreRequest(String username, String domain, String host, HqAuth auth){
        this.username = username;
        this.domain = domain;
        this.host = host;
        this.auth = auth;
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

    public HqAuth getAuth() {
        return auth;
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
