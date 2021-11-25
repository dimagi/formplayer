package org.commcare.formplayer.web.client;

import com.google.common.collect.Multimap;
import org.commcare.formplayer.services.RestoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class WebClient {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestoreFactory restoreFactory;

    public String get(String url) {
        return restTemplate.exchange(
                RequestEntity.get(url).headers(restoreFactory.getUserHeaders()).build(), String.class
        ).getBody();
    }

    public String get(URI uri) {
        return restTemplate.exchange(
                RequestEntity.get(uri).headers(restoreFactory.getUserHeaders()).build(), String.class
        ).getBody();
    }

    public <T> String post(String url, T body) {
        return restTemplate.exchange(
                RequestEntity.post(url).headers(restoreFactory.getUserHeaders()).body(body), String.class
        ).getBody();
    }

    public <T> String postFormData(String url, Multimap<String, String> data) {
        return restTemplate.exchange(
                RequestEntity.post(url)
                             .headers(restoreFactory.getUserHeaders())
                             .contentType(MediaType.MULTIPART_FORM_DATA)
                             .body(data),
                String.class
        ).getBody();
    }

}
