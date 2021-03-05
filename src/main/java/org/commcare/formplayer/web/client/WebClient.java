package org.commcare.formplayer.web.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class WebClient {

    @Autowired
    RestTemplate restTemplate;

    public String get(String url, HttpHeaders headers) {
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<String>(headers), String.class
        );
        return response.getBody();
    }

    public String get(URI uri, HttpHeaders headers) {
        ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                String.class
        );
        return response.getBody();
    }
}
