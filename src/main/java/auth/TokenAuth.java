package auth;

import org.springframework.http.HttpHeaders;
import util.Constants;

/**
 * Created by benrudolph on 2/1/17.
 */
public class TokenAuth implements HqAuth{

    private final String token;

    public TokenAuth(String token) {
        this.token = token;
    }

    @Override
    public HttpHeaders getAuthHeaders() {
        return new HttpHeaders(){
            {
                add("Authorization",  "Token " + token);
            }
        };
    }

    @Override
    public String toString(){
        return "TokenAuth key=" + token;
    }
}
