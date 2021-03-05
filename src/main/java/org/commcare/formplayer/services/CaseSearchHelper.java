package org.commcare.formplayer.services;

import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@CacheConfig(cacheNames = "case_search")
public class CaseSearchHelper {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    QueryRequester queryRequester;


    @Cacheable(unless = "#result == null", key = "#cacheKey")
    public ExternalDataInstance getSearchDataInstance(String cacheKey, FormplayerQueryScreen screen, String uri) {
        String responseString = queryRequester.makeQueryRequest(uri, restoreFactory.getUserHeaders());
        if (responseString != null) {
            Pair<ExternalDataInstance, String> dataInstanceWithError = screen.processResponse(
                    new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)));
            return dataInstanceWithError.first;
        }
        return null;
    }

    public String getCacheKey(String uri) {
        StringBuilder builder = new StringBuilder();
        builder.append(restoreFactory.getDomain());
        builder.append("_").append(restoreFactory.getScrubbedUsername());
        if (restoreFactory.getAsUsername() != null) {
            builder.append("_").append(restoreFactory.getAsUsername());
        }
        builder.append("_").append(uri);
        return builder.toString();
    }
}
