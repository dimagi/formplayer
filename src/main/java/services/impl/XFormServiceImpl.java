package services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import services.AuthService;
import services.XFormService;

/**
 * Created by willpride on 1/20/16.
 */
public class XFormServiceImpl implements XFormService {

    @Autowired
    AuthService authService;

    @Override
    public String getFormXml(String formUrl) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(formUrl,
                        HttpMethod.GET,
                        new HttpEntity<String>(authService.getAuth().getAuthHeaders()), String.class);
        return response.getBody();
    }
}
