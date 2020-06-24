package org.commcare.formplayer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class XFormService {

    @Autowired
    RestoreFactory restoreFactory;

    public String getFormXml(String formUrl) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(formUrl,
                        HttpMethod.GET,
                        new HttpEntity<String>(restoreFactory.getUserHeaders()), String.class);
        return response.getBody();
    }
}
