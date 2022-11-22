package org.commcare.formplayer.services;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.web.client.WebClient;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Created by willpride on 10/4/17.
 */
@Service
@CommonsLog
public class FormplayerFormSendCalloutHandler implements FormSendCalloutHandler {

    @Autowired
    private WebClient webClient;

    @Override
    public String performHttpCalloutForResponse(String url, Multimap<String, String> paramMap) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (Map.Entry<String, String> param : paramMap.entries()) {
            builder.queryParam(param.getKey(), param.getValue());
        }
        String responseBody = webClient.get(builder.build().toUri());
        log.info(String.format("Form HttpCallout to URL %s returned result %s", url, responseBody));
        return responseBody;
    }
}
