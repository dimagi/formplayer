package services.impl;

import annotations.MethodMetrics;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import services.RestoreFactory;
import services.SyncRequester;
import util.Constants;

import java.util.Arrays;

/**
 * Created by willpride on 4/26/17.
 */
public class SyncRequesterImpl implements SyncRequester {

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    protected RestoreFactory restoreFactory;


    private final Log log = LogFactory.getLog(SyncRequesterImpl.class);

    @Override
    @MethodMetrics
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
