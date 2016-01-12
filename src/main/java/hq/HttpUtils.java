package hq;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpHeaders;

import java.nio.charset.Charset;

/**
 * Created by willpride on 1/12/16.
 */
public class HttpUtils {
    public static String getFullUsername(String username, String domain){
        // assumes commcarehq.org as server
        return username + "@" + domain + ".commcarehq.org";
    }

    public static HttpHeaders createHeaders(String username, String password ){
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
}
