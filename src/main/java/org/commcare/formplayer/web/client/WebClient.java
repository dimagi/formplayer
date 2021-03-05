package org.commcare.formplayer.web.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;

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

    public String get(String url, String params, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity<String> entity = new HttpEntity<>(params, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }
}
