package services.impl;

import auth.HqAuth;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import services.RestoreService;

/**
 * Created by willpride on 1/21/16.
 */
public class RestoreServiceImpl implements RestoreService {
    @Override
    public String getRestoreXml(String host, String domain, HqAuth auth) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange("http://" + host
                                + "/a/" + domain + "/phone/restore/?version=2.0",
                        HttpMethod.GET,
                        new HttpEntity<String>(auth.getAuthHeaders()), String.class);
        return response.getBody();
    }
}
