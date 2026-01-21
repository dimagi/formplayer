package org.commcare.formplayer.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.CaseInstanceXmlTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.formplayer.DbUtils;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.sandbox.CaseSearchSqlSandbox;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.sqlitedb.CaseSearchDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.util.screen.ScreenUtils;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.utils.TreeUtilities;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.MD5;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@CacheConfig(cacheNames = "case_search")
@Component
public class CaseSearchHelper {

    private static final String CASE_SEARCH_INDEX_TABLE_PREFIX = "case_search_index_storage_";
    @Autowired
    CacheManager cacheManager;

    @Autowired
    private RestoreFactory restoreFactory;
    @Autowired
    private WebClient webClient;

    @Autowired
    private FormplayerStorageFactory storageFactory;
    private final Log log = LogFactory.getLog(CaseSearchHelper.class);

    public IStorageUtilityIndexed<Case> getCaseSearchStorage(ExternalDataInstanceSource source)
            throws InvalidStructureException {
        Multimap<String, String> requestData = source.getRequestData();
        String cacheKey = getCacheKey(source.getSourceUri(), requestData);

        CaseSearchDB caseSearchDb = initCaseSearchDB();
        String caseSearchTableName = evalCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        return caseSearchStorage;
    }

    public AbstractTreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source,
            boolean skipCache)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException,
            IOException {

        Multimap<String, String> requestData = source.getRequestData();
        String url = source.getSourceUri();

        Cache cache = cacheManager.getCache("case_search");
        String cacheKey = getCacheKey(source.getSourceUri(), requestData);
        TreeElement cachedRoot = getCachedRoot(cache, cacheKey, url, skipCache);
        if (cachedRoot != null) {
            return cachedRoot;
        }

        CaseSearchDB caseSearchDb = initCaseSearchDB();
        String caseSearchTableName = evalCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        FormplayerCaseIndexTable caseSearchIndexTable = getCaseIndexTable(caseSearchSandbox, caseSearchTableName);
        if (skipCache || !caseSearchStorage.isStorageExists()) {
            String responseString = webClient.postFormData(url, requestData);
            if (responseString != null) {
                byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream responeStream = new ByteArrayInputStream(responseBytes);
                if (shouldParseIntoCaseSearchStorage(source.useCaseTemplate())) {
                    parseIntoCaseSearchStorage(caseSearchDb, caseSearchSandbox, caseSearchStorage, responeStream,
                            caseSearchIndexTable);
                } else {
                    TreeElement root = TreeUtilities.xmlStreamToTreeElement(responeStream, instanceId);
                    if (root != null) {
                        cache.put(cacheKey, root);
                    }
                    return root;
                }
            }
        }

        if (caseSearchStorage.isStorageExists()) {
            // return root as CaseInstanceTreeElement
            InstanceBase instanceBase = new InstanceBase(instanceId);
            return new CaseInstanceTreeElement(instanceBase, caseSearchStorage, caseSearchIndexTable);
        }

        throw new IOException("No response from server for case search query");
    }

    private FormplayerCaseIndexTable getCaseIndexTable(ConnectionHandler caseSearchSandbox,
            String caseSearchTableName) {
        String caseSearchIndexTableName = CASE_SEARCH_INDEX_TABLE_PREFIX + caseSearchTableName;
        return new FormplayerCaseIndexTable(
                caseSearchSandbox, caseSearchIndexTableName, caseSearchTableName, false);
    }

    private CaseSearchDB initCaseSearchDB() {
        return new CaseSearchDB(restoreFactory.getDomain(), restoreFactory.getUsername(),
                restoreFactory.getAsUsername());
    }

    private static String evalCaseSearchTableName(String cacheKey) {
        return UserSqlSandbox.FORMPLAYER_CASE + "_" + MD5.toHex(
                MD5.hash(cacheKey.getBytes(StandardCharsets.UTF_8)));
    }

    private void parseIntoCaseSearchStorage(SQLiteDB caseSearchDb, UserSqlSandbox caseSearchSandbox,
            IStorageUtilityIndexed<Case> caseSearchStorage, ByteArrayInputStream responeStream,
            FormplayerCaseIndexTable caseSearchIndexTable)
            throws UnfullfilledRequirementsException, InvalidStructureException,
            XmlPullParserException, IOException {
        try {
            DbUtils.setAutoCommit(caseSearchDb, false);
            caseSearchIndexTable.delete();
            caseSearchIndexTable.createTable();
            CaseInstanceXmlTransactionParserFactory factory = new CaseInstanceXmlTransactionParserFactory(
                    caseSearchSandbox, caseSearchIndexTable);
            caseSearchStorage.initStorage();
            caseSearchStorage.removeAll();
            ParseUtils.parseIntoSandbox(responeStream, factory, true, true);
            DbUtils.commit(caseSearchDb);
        } catch (Exception e) {
            DbUtils.rollback(caseSearchDb);
            throw e;
        } finally {
            DbUtils.setAutoCommit(caseSearchDb, true);
        }
    }

    private boolean shouldParseIntoCaseSearchStorage(boolean useCaseTemplate) {
        return useCaseTemplate && storageFactory.getPropertyManager().isIndexCaseSearchResults();
    }

    private TreeElement getCachedRoot(Cache cache, String cacheKey, String url, boolean skipCache) {
        if (skipCache) {
            log.info(String.format("Skipping cache check for case search results. Key: %s", cacheKey));
        } else {
            TreeElement cachedRoot = cache.get(cacheKey, TreeElement.class);
            if (cachedRoot != null) {
                log.info(String.format("Cache HIT for case search: url=%s, key=%s", url, cacheKey));
                // Deep copy to avoid concurrency issues
                TreeElement copyOfRoot = SerializationUtil.deserialize(ExtUtil.serialize(cachedRoot),
                        TreeElement.class);
                return copyOfRoot;
            } else {
                log.info(String.format("Cache MISS for case search: url=%s, key=%s", url, cacheKey));
            }
        }
        return null;
    }

    public ExternalDataInstance getRemoteDataInstance(String instanceId, boolean useCaseTemplate, URL url,
            Multimap<String, String> requestData, boolean skipCache)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException,
            IOException {

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

        CaseSearchDB caseSearchDb = initCaseSearchDB();
        String caseSearchTableName = evalCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        caseSearchStorage.deleteStorage();
        FormplayerCaseIndexTable caseSearchIndexTable = getCaseIndexTable(caseSearchSandbox, caseSearchTableName);
        caseSearchIndexTable.delete();
    }

    private String getCacheKey(String url, Multimap<String, String> queryParams) throws InvalidStructureException {
        URI uri;
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
        
        // Include user authorization context in cache key to prevent superusers and domain members
        // from sharing cached case search results, as they may have different access permissions
        Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
        if (userDetails.isPresent()) {
            HqUserDetailsBean user = userDetails.get();
            String domain = restoreFactory.getDomain();
            String username = restoreFactory.getScrubbedUsername();
            
            // Include superuser status
            boolean isSuperUser = user.isSuperUser();
            builder.append("_superuser=").append(isSuperUser);
            
            // Include domain membership status (superusers can access any domain, members must be authorized)
            boolean isDomainMember = user.isAuthorized(domain, username);
            builder.append("_member=").append(isDomainMember);
            
            log.debug(String.format(
                "Case search cache key includes auth context: domain=%s, user=%s, isSuperUser=%s, isDomainMember=%s",
                domain, username, isSuperUser, isDomainMember));
        }
        
        builder.append("_").append(uri);
        Map<String, Collection<String>> sortedQueryParams = new TreeMap<>(queryParams.asMap());
        for (String key : sortedQueryParams.keySet()) {
            builder.append("_").append(key);
            Collection<String> values = queryParams.get(key);
            List<String> valuesList = values.stream().sorted().toList();
            for (String value : valuesList) {
                builder.append("=").append(value);
            }
        }
        String cacheKey = builder.toString();
        log.debug(String.format("Generated case search cache key: %s", cacheKey));
        return cacheKey;
    }

    public void clearCache() {
        cacheManager.getCache("case_search").clear();
    }

    public Multimap<String, String> getMetricTags(MenuSession menuSession) {
        String moduleNameTagValue = ScreenUtils.getBestTitle(menuSession.getSessionWrapper());

        Multimap<String, String> multimap = ArrayListMultimap.create();
        if (moduleNameTagValue != null && !moduleNameTagValue.isEmpty()) {
            multimap.put("x_commcare_tag_module_name", moduleNameTagValue);
        }

        return multimap;
    }
}
