package auth;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import util.StringUtils;

import java.nio.charset.Charset;

/**
 * Created by willpride on 1/13/16.
 */
public class BasicAuth implements HqAuth {
    private final String username;
    private final String password;
    private final String domain;
    private final String host;

    public BasicAuth(String username, String domain, String host, String password ){
        this.username = username;
        this.domain = domain;
        this.host = host;
        this.password = password;
    }

    @Override
    public HttpHeaders getAuthHeaders() {
        return new HttpHeaders(){
            {
                String auth = StringUtils.getFullUsername(username, domain, host) + ":" + password;
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
}
