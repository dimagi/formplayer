package screens;

import auth.HqAuth;
import org.commcare.util.screen.QueryScreen;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

/**
 * Created by willpride on 8/7/16.
 */
public class FormplayerQueryScreen extends QueryScreen {

    public FormplayerQueryScreen(){
        super();
    }

    public String getUriString() {
        URL url = getBaseUrl();
        Hashtable<String, String> params = getQueryParams();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url.toString());
        for(String key: params.keySet()){
            builder.queryParam(key, params.get(key));
        }
        return builder.toUriString();
    }
}
