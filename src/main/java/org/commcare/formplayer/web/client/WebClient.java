package org.commcare.formplayer.web.client;

import com.google.common.collect.Multimap;
import lombok.extern.apachecommons.CommonsLog;
import org.commcare.formplayer.services.RestoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
@CommonsLog
public class WebClient {

    RestTemplate restTemplate;

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
        ResponseEntity<T> response = null;
        try {
            response = restTemplate.exchange(
                    RequestEntity.get(uri).headers(restoreFactory.getRequestHeaders(uri)).build(),
                    responseType
            );
        } finally {
            HttpStatus status = response == null ? null : response.getStatusCode();
            log.info(String.format("HTTP GET to '%s'. Response %s", uri, status));
        }
        return response;
    }

    public <T> String post(String url, T body) {
        return post(url, body, false);
    }

    public <T> String post(String url, T body, boolean isMultipart) {
        URI uri = URI.create(url);
        HttpHeaders headers = restoreFactory.getRequestHeaders(uri);
        if (isMultipart) {
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        }
        return postRaw(uri, headers, body, String.class).getBody();
    }

    public <T> String postFormData(String url, Multimap<String, String> data) {
        URI uri = URI.create(url);
        LinkedMultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
        data.forEach(postData::add);

        HttpHeaders headers = restoreFactory.getRequestHeaders(uri);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return postRaw(uri, headers, postData, String.class).getBody();
    }

    public <T> Boolean caseClaimPost(String url, T body) {
        URI uri = URI.create(url);
        ResponseEntity<String> entity = postRaw(uri, restoreFactory.getRequestHeaders(uri), body, String.class);
        Boolean shouldSync = true;
        if (entity != null && entity.getStatusCode() == HttpStatus.NO_CONTENT) {
            shouldSync = false;
        }
        return shouldSync;
    }

    public <T, R> ResponseEntity<R> postRaw(URI uri, HttpHeaders headers, T body, Class<R> responseType) {
        ResponseEntity<R> response = null;

        try {
            response = restTemplate.exchange(
                    RequestEntity.post(uri).headers(headers).body(body),
                    responseType
            );
        } finally {
            HttpStatus status = response == null ? null : response.getStatusCode();
            log.info(String.format("HTTP POST to '%s'. Response %s. Request body '%s'", uri, status, body));
        }

        return response;
    }

    @Autowired
    public void setRestoreFactory(RestoreFactory restoreFactory) {
        this.restoreFactory = restoreFactory;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
