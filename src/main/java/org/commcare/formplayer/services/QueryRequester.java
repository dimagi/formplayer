package org.commcare.formplayer.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;


@Component
@CommonsLog
public class QueryRequester {

    @Autowired
    private RestTemplate restTemplate;

    public String makeQueryRequest(URI uri, HttpHeaders headers) {
        ResponseEntity<String> response;
        response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                String.class
        );
        String responseBody = response.getBody();
        log.info(String.format("Query request to URL %s successful", uri));
        return responseBody;
    }
}
