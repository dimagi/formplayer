package requests;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;

/**
 * Created by willpride on 1/12/16.
 */
public class FilterRequest {
    String filter;
    String username;
    String authKey;
    String domain;
    String host;

    public FilterRequest(String body, HttpHeaders header){
        JSONObject jsonBody = new JSONObject(body);
        filter = jsonBody.getString("filter_expr");
        JSONObject sessionData = jsonBody.getJSONObject("session_data");
        username = sessionData.getString("username");
        JSONObject auth = jsonBody.getJSONObject("hq_auth");
        authKey = auth.getString("key");
        host = header.getFirst("host");
        domain = sessionData.getString("domain");
    }

    public String getFilter(){
        return filter;
    }

    public String getUsername(){
        return username;
    }

    public String getAuthKey(){
        return authKey;
    }

    public String getHost() {
        return host;
    }

    public String getDomain() {
        return domain;
    }
}
