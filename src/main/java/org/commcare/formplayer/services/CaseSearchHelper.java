package org.commcare.formplayer.services;

import com.google.common.collect.Multimap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
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
import java.net.URISyntaxException;
import java.net.URL;
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

    public AbstractTreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source, boolean skipCache)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException {

        Multimap<String, String> requestData = source.getRequestData();
        String url = source.getSourceUri();

        Cache cache = cacheManager.getCache("case_search");
        String cacheKey = getCacheKey(source.getSourceUri(), requestData);
        TreeElement cachedRoot = null;
        if (skipCache) {
            log.info("Skipping cache check for case search results");
        } else {
            cachedRoot = cache.get(cacheKey, TreeElement.class);
        }
        if (cachedRoot != null) {
            log.info(String.format("Using cached case search results for %s", url));
            // Deep copy to avoid concurrency issues
            TreeElement copyOfRoot = SerializationUtil.deserialize(ExtUtil.serialize(cachedRoot),
                    TreeElement.class);
            return copyOfRoot;
        }

        String responseString = webClient.postFormData(url, requestData);

        if (responseString != null) {
            TreeElement root = ExternalDataInstance.parseExternalTree(
                    new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)), instanceId);
            if (root != null) {
                cache.put(cacheKey, root);
            }
            return root;
        }

        throw new IOException("No response from server for case search query");
    }

    public ExternalDataInstance getRemoteDataInstance(String instanceId, boolean useCaseTemplate, URL url,
            Multimap<String, String> requestData, boolean skipCache)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException {

        ExternalDataInstanceSource source = ExternalDataInstanceSource.buildRemote(
                instanceId, null, useCaseTemplate, url.toString(), requestData);

        AbstractTreeElement root = getExternalRoot(instanceId, source, skipCache);
        source.init(root);

        return source.toInstance();
    }

    public void clearCacheForInstanceSource(ExternalDataInstanceSource source) throws InvalidStructureException {
        if (source.getSourceUri() == null) {
            return;
        }
        String cacheKey = getCacheKey(source.getSourceUri(), source.getRequestData());
        Cache cache = cacheManager.getCache("case_search");
        cache.evict(cacheKey);
    }

    private String getCacheKey(String url, Multimap<String, String> queryParams) throws InvalidStructureException {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new InvalidStructureException("Invalid URI in source: " + url);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(restoreFactory.getDomain());
        builder.append("_").append(restoreFactory.getScrubbedUsername());
        if (restoreFactory.getAsUsername() != null) {
            builder.append("_").append(restoreFactory.getAsUsername());
        }
        builder.append("_").append(uri);
        for (String key : queryParams.keySet()) {
            builder.append("_").append(key);
            for (String value : queryParams.get(key)) {
                builder.append("=").append(value);
            }
        }
        return builder.toString();
    }
}
