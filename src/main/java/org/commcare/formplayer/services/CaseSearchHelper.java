package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Component;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@CacheConfig(cacheNames = "case_search")
@Component
public class CaseSearchHelper implements RemoteInstanceFetcher {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private WebClient webClient;

    private final Log log = LogFactory.getLog(CaseSearchHelper.class);

    @Override
    public ExternalDataInstance getRemoteDataInstance(String instanceId, boolean useCaseTemplate, URI uri)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException {
        Cache cache = cacheManager.getCache("case_search");
        String cacheKey = getCacheKey(uri);
        TreeElement cachedRoot = cache.get(cacheKey, TreeElement.class);
        if (cachedRoot != null) {
            // Deep copy to avoid concurrency issues
            TreeElement copyOfRoot = SerializationUtil.deserialize(ExtUtil.serialize(cachedRoot), TreeElement.class);
            return ExternalDataInstance.buildFromRemote(
                    instanceId,
                    copyOfRoot,
                    uri.toString(),
                    useCaseTemplate);
        }
        log.info(String.format("Making case search request to url %s", uri));
        String responseString = webClient.get(uri);
        if (responseString != null) {
            ExternalDataInstance instance = ExternalDataInstance.buildFromRemote(instanceId, new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)),
                    uri.toString(), useCaseTemplate);
            TreeElement root = (TreeElement)instance.getRoot();
            if (root != null) {
                cache.put(cacheKey, root);
            }
            return instance;
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
