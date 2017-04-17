package services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import services.AuthService;
import services.SubmitService;

/**
 * RestTemplate implementation for submitting forms to HQ
 */
public class SubmitServiceImpl implements SubmitService {

    @Autowired
    AuthService authService;

    @Override
    public ResponseEntity<String> submitForm(String formXml, String submitUrl) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, authService.getAuth().getAuthHeaders());
        ResponseEntity<String> response =
                restTemplate.exchange(submitUrl,
                        HttpMethod.POST,
                        entity, String.class);
        return response;
    }
}
