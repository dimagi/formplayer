package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@CacheConfig(cacheNames = {"case_search"})
public class CaseSearchHelper {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    QueryRequester queryRequester;

    @Cacheable(unless="#result == null", key="#username + #uri")
    public ExternalDataInstance getSearchDataInstance(String username, FormplayerQueryScreen screen, String uri, HttpHeaders headers) {
        String responseString = queryRequester.makeQueryRequest(uri, headers);
        if (responseString != null) {
            Pair<ExternalDataInstance, String> dataInstanceWithError = screen.processResponse(
                    new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)));
            return dataInstanceWithError.first;
        }
        return null;
    }

    @CacheEvict(allEntries = true)
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void reportCacheEvict() {
        System.out.println("Flushed case_search cache");
    }
}
