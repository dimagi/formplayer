package org.commcare.formplayer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class XFormService {

    @Autowired
    RestoreFactory restoreFactory;

    @Autowired
    RestTemplate restTemplate;

    public String getFormXml(String formUrl) {
        ResponseEntity<String> response =
                restTemplate.exchange(formUrl,
                        HttpMethod.GET,
                        new HttpEntity<String>(restoreFactory.getUserHeaders()), String.class);
        return response.getBody();
    }
}
