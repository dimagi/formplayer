package requests;

import org.json.JSONObject;

/**
 * Created by willpride on 1/12/16.
 */
public class FilterRequest {
    String filter;
    String username;
    String authKey;

    public FilterRequest(String body){
        JSONObject jsonBody = new JSONObject(body);
        filter = jsonBody.getString("filter_expr");
        JSONObject sessionData = jsonBody.getJSONObject("session_data");
        username = sessionData.getString("username");
        JSONObject auth = jsonBody.getJSONObject("hq_auth");
        authKey = auth.getString("key");
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
}
