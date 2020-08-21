package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
public class SyncRequester {

    @Autowired
    RestTemplate restTemplate;

    private final Log log = LogFactory.getLog(SyncRequester.class);

    public ResponseEntity<String> makeSyncRequest(String url, String params, HttpHeaders headers) {
        log.info(String.format("SyncRequester with url %s and  params %s and headers %s", url, params, headers));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity<String> entity = new HttpEntity<>(params, headers);
        ResponseEntity<String> response =
                restTemplate.exchange(url,
                        HttpMethod.POST,
                        entity, String.class);
        log.info(String.format("SyncRequest gave response %s", response));
        return response;
    }
}
