package requests;

import auth.BasicAuth;
import hq.RestoreUtils;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;

/**
 * Created by willpride on 1/12/16.
 */
public class FilterRequest extends RestoreRequest{

    String filter;

    public FilterRequest(String body) {
        super(body);
        JSONObject jsonBody = new JSONObject(body);
        filter = jsonBody.getString("filter_expr");
    }

    public FilterRequest(String username, String password, String domain, String host, String filter){
        super(username, password, domain, host);
        this.filter = filter;
    }

    public String getFilter(){
        return filter;
    }
}
