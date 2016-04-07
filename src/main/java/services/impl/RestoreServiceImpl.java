package services.impl;

import auth.HqAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import services.RestoreService;

/**
 * Created by willpride on 1/21/16.
 */
@Component
public class RestoreServiceImpl implements RestoreService {

    @Value("${commcarehq.host}")
    String host;

    @Override
    public String getRestoreXml(String domain, HqAuth auth) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(host
                                + "/a/" + domain + "/phone/restore/?version=2.0",
                        HttpMethod.GET,
                        new HttpEntity<String>(auth.getAuthHeaders()), String.class);
        return response.getBody();
    }
}
