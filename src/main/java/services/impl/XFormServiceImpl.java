package services.impl;

import auth.HqAuth;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import services.XFormService;

/**
 * Created by willpride on 1/20/16.
 */
public class XFormServiceImpl implements XFormService{

    @Override
    public String getFormXml(String formUrl, HqAuth hqAuth) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(formUrl,
                        HttpMethod.GET,
                        new HttpEntity<String>(hqAuth.getAuthHeaders()), String.class);
        return response.getBody();
    }
}
