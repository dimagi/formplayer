package services.impl;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import services.SyncRequester;

import java.util.Arrays;

/**
 * Created by willpride on 4/26/17.
 */
public class SyncRequesterImpl implements SyncRequester {
    @Override
    public ResponseEntity<String> makeSyncRequest(String url, String params, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity<String> entity = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(url,
                        HttpMethod.POST,
                        entity, String.class);
        return response;
    }
}
