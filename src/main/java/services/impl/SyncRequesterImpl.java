package services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import services.SyncRequester;

import java.util.Arrays;

/**
 * Created by willpride on 4/26/17.
 */
public class SyncRequesterImpl implements SyncRequester {

    private final Log log = LogFactory.getLog(SyncRequesterImpl.class);

    @Override
    public ResponseEntity<String> makeSyncRequest(String url, String params, HttpHeaders headers) {
        log.info(String.format("SyncRequester with url %s and  params %s and headers %s", url, params, headers));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity<String> entity = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(url,
                        HttpMethod.POST,
                        entity, String.class);
        log.info(String.format("SyncRequest gave response %s", response));
        return response;
    }
}
