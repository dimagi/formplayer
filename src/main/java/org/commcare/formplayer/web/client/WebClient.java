package org.commcare.formplayer.web.client;

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

    public String get(String url, HttpHeaders headers) {
        return restTemplate.exchange(
                RequestEntity.get(url).headers(headers).build(), String.class
        ).getBody();
    }

    public String get(URI uri, HttpHeaders headers) {
        return restTemplate.exchange(
                RequestEntity.get(uri).headers(headers).build(), String.class
        ).getBody();
    }

    public <T> String post(String url, T body, HttpHeaders headers) {
        return restTemplate.exchange(
                RequestEntity.post(url).headers(headers).body(body), String.class
        ).getBody();
    }
}
