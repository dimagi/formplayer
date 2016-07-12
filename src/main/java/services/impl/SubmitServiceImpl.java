package services.impl;

import auth.HqAuth;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import services.SubmitService;

/**
 * RestTemplate implementation for submitting forms to HQ
 */
public class SubmitServiceImpl implements SubmitService{

    @Override
    public ResponseEntity<String> submitForm(String formXml, String submitUrl, HqAuth auth) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<Object>(formXml, auth.getAuthHeaders());
        ResponseEntity<String> response =
                restTemplate.exchange(submitUrl,
                        HttpMethod.POST,
                        entity, String.class);
        return response;
    }
}
