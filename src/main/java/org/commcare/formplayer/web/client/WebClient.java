package org.commcare.formplayer.web.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class WebClient {

    @Autowired
    RestTemplate restTemplate;

    public String get(String url, HttpHeaders headers) {
        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get(url).headers(headers).build(), String.class
        );
        return response.getBody();
    }

    public String get(URI uri, HttpHeaders headers) {
        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get(uri).headers(headers).build(), String.class
        );
        return response.getBody();
    }

    public String postFormData(String url, MultiValueMap<String, String> formData, HttpHeaders headers) {
        RequestEntity<MultiValueMap<String, String>> request = RequestEntity.post(url)
                .headers(headers)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData);
        return restTemplate.exchange(request, String.class).getBody();
    }
}
