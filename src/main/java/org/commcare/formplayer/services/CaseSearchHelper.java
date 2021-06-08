package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.aspects.LockAspect;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@CacheConfig(cacheNames = "case_search")
@Component
public class CaseSearchHelper {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private WebClient webClient;

    private final Log log = LogFactory.getLog(CaseSearchHelper.class);

    public ExternalDataInstance getSearchDataInstance(FormplayerQueryScreen screen, String uri) {
        Cache cache = cacheManager.getCache("case_search");
        String cacheKey = getCacheKey(uri);
        TreeElement cachedRoot = cache.get(cacheKey, TreeElement.class);
        if (cachedRoot != null) {
            // Deep copy to avoid concurrency issues
            TreeElement copyOfRoot = SerializationUtil.deserialize(ExtUtil.serialize(cachedRoot), TreeElement.class);
            return screen.buildExternalDataInstance(copyOfRoot);
        }
        log.info(String.format("Making case search request to url %s",  uri));
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

    private String getCacheKey(String uri) {
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
