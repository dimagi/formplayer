package requests;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import org.json.JSONObject;

/**
 * Created by willpride on 1/14/16.
 */
public class AuthRequest {

    HqAuth auth;

    public AuthRequest(String body) {
        JSONObject jsonBody = new JSONObject(body);
        JSONObject authJson = jsonBody.getJSONObject("hq_auth");
        String authKey = authJson.getString("key");
        auth = new DjangoAuth(authKey);
    }

    public AuthRequest(String username, String password){
        this.auth = new BasicAuth(username, password);
    }

    public HqAuth getAuth(){
        return auth;
    }

    public JSONObject getSessionData(JSONObject body){
        if(body.has("session-data")){
            return body.getJSONObject("session-data");
        }
        return body.getJSONObject("session_data");
    }
}
