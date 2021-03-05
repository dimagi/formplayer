package org.commcare.formplayer.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.web.client.WebClient;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * Created by willpride on 10/4/17.
 */
@Service
@CommonsLog
public class FormplayerFormSendCalloutHandler implements FormSendCalloutHandler {

    @Autowired
    RestoreFactory restoreFactory;

    @Autowired
    private WebClient webClient;

    @Override
    public String performHttpCalloutForResponse(String url, Map<String, String> paramMap) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (String key: paramMap.keySet()) {
            builder.queryParam(key, paramMap.get(key));
        }

        String responseBody = webClient.get(builder.build().toUri(), restoreFactory.getUserHeaders());
        log.info(String.format("Form HttpCallout to URL %s returned result %s", url, responseBody));
        return responseBody;
    }
}
