package org.commcare.formplayer.web.client;

import org.commcare.formplayer.services.RestoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
        return get(uri, String.class);
    }

    public String get(URI uri) {
        return get(uri, String.class);
    }

    public <T> T get(URI uri, Class<T> responseType) {
        return getRaw(uri, responseType).getBody();
    }

    public <T> ResponseEntity<T> getRaw(URI uri, Class<T> responseType) {
        return restTemplate.exchange(
                RequestEntity.get(uri).headers(restoreFactory.getRequestHeaders(uri)).build(), responseType
        );
    }

    public <T> String post(String url, T body) {
        URI uri = URI.create(url);
        return restTemplate.exchange(
                RequestEntity.post(uri).headers(restoreFactory.getRequestHeaders(uri)).body(body), String.class
        ).getBody();
    }
}
