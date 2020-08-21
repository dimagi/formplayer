package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class FormplayerFormSendCalloutHandler implements FormSendCalloutHandler {

    private final Log log = LogFactory.getLog(QueryRequester.class);

    @Autowired
    RestoreFactory restoreFactory;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public String performHttpCalloutForResponse(String url, Map<String, String> paramMap) {
        ResponseEntity<String> response = null;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (String key: paramMap.keySet()) {
            builder.queryParam(key, paramMap.get(key));
        }

        try {
            response = restTemplate.exchange(
                    // Spring framework automatically encodes urls. This ensures we don't pass in an already
                    // encoded url.
                    URLDecoder.decode(builder.toUriString(), "UTF-8"),
                    HttpMethod.GET,
                    new HttpEntity<String>(restoreFactory.getUserHeaders()),
                    String.class
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String responseBody = response.getBody();
        log.info(String.format("Form HttpCallout to URL %s returned result %s", url, responseBody));
        return responseBody;
    }
}
