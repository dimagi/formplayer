package org.commcare.formplayer.web.client;

import org.commcare.formplayer.services.RestoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
        URI uri = URI.create(url);
        return restTemplate.exchange(
                RequestEntity.get(uri).headers(restoreFactory.getRequestHeaders(uri)).build(), String.class
        ).getBody();
    }

    public String get(URI uri) {
        return restTemplate.exchange(
                RequestEntity.get(uri).headers(restoreFactory.getRequestHeaders(uri)).build(), String.class
        ).getBody();
    }

    public <T> String post(String url, T body) {
        URI uri = URI.create(url);
        return restTemplate.exchange(
                RequestEntity.post(uri).headers(restoreFactory.getRequestHeaders(uri)).body(body), String.class
        ).getBody();
    }
}
