package org.commcare.formplayer.services;

import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@CacheConfig(cacheNames = "case_search")
public class CaseSearchHelper {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private WebClient webClient;


    public ExternalDataInstance getSearchDataInstance(FormplayerQueryScreen screen, URI uri) {
        Cache cache = cacheManager.getCache("case_search");
        String cacheKey = getCacheKey(uri);
        TreeElement cachedRoot = cache.get(cacheKey, TreeElement.class);
        if (cachedRoot != null) {
            // Deep copy to avoid concurrency issues
            TreeElement copyOfRoot = SerializationUtil.deserialize(SerializationUtil.serialize(cachedRoot), TreeElement.class);
            return screen.buildExternalDataInstance(copyOfRoot);
        }

        String responseString = webClient.get(uri);
        if (responseString != null) {
            Pair<ExternalDataInstance, String> dataInstanceWithError = screen.processResponse(
                    new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)));
            if (dataInstanceWithError.first != null) {
                TreeElement root = (TreeElement)dataInstanceWithError.first.getRoot();
                if (root != null) {
                    cache.put(cacheKey, root);
                }
            }
            return dataInstanceWithError.first;
        }
        return null;
    }

    private String getCacheKey(URI uri) {
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
