package auth;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import util.StringUtils;

import java.nio.charset.Charset;

/**
 * Represents a Basic Authentication credential for a user
 * Currently used for SMS requests
 */
public class BasicAuth implements HqAuth {
    private final String username;
    private final String password;

    public BasicAuth(String username, String password){
        this.username = username;
        this.password = password;
    }

    @Override
    public HttpHeaders getAuthHeaders() {
        return new HttpHeaders(){
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")) );
                String authHeader = "Basic " + new String( encodedAuth );
                set( "Authorization", authHeader );
            }
        };
    }

    @Override
    public String toString(){
        return "BasicAuth [username=" + username + "]";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
