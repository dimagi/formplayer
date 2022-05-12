package org.commcare.formplayer.services;

import com.google.common.collect.Multimap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
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

    public TreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException {

        Multimap<String, String> requestData = source.getRequestData();
        String url = source.getSourceUri();

        Cache cache = cacheManager.getCache("case_search");
        URI uri = null;
        try {
            uri = new URI(source.getSourceUri());
        } catch (URISyntaxException e) {
            throw new InvalidStructureException("Invalid URI in source: " + uri);
        }
        String cacheKey = getCacheKey(uri, requestData);
        TreeElement cachedRoot = cache.get(cacheKey, TreeElement.class);
        if (cachedRoot != null) {
            log.info(String.format("Using cached case search results for %s", uri));
            // Deep copy to avoid concurrency issues
            TreeElement copyOfRoot = SerializationUtil.deserialize(ExtUtil.serialize(cachedRoot), TreeElement.class);
            return copyOfRoot;
        }

        log.info(String.format("Making case search request to url %s with data %s",  url, requestData));
        String responseString = webClient.postFormData(url, requestData);

        if (responseString != null) {
            TreeElement root = ExternalDataInstance.parseExternalTree(new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)), instanceId);
            if (root != null) {
                cache.put(cacheKey, root);
            }
            return root;
        }

        throw new IOException("No response from server for case search query");
    }

    public ExternalDataInstance getRemoteDataInstance(String instanceId, boolean useCaseTemplate, URL url, Multimap<String, String> requestData)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException {

        ExternalDataInstanceSource source = ExternalDataInstanceSource.buildRemote(
                instanceId, null, useCaseTemplate, url.toString(), requestData);

        TreeElement root = getExternalRoot(instanceId, source);
        source.init(root);

        return source.toInstance();
    }

    private String getCacheKey(URI url, Multimap<String, String> queryParams) {
        StringBuilder builder = new StringBuilder();
        builder.append(restoreFactory.getDomain());
        builder.append("_").append(restoreFactory.getScrubbedUsername());
        if (restoreFactory.getAsUsername() != null) {
            builder.append("_").append(restoreFactory.getAsUsername());
        }
        builder.append("_").append(url);
        for (String key : queryParams.keySet()) {
            builder.append("_").append(key);
            for (String value : queryParams.get(key)) {
                builder.append("=").append(value);
            }
        }
        return builder.toString();
    }
}
