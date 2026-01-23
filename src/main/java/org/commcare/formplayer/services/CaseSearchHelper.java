package org.commcare.formplayer.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.sentry.SentryLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.CaseInstanceXmlTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.formplayer.DbUtils;
import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.sandbox.CaseSearchSqlSandbox;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.sqlitedb.CaseSearchDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.util.FormplayerSentry;
import org.commcare.formplayer.util.SerializationUtil;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.util.screen.ScreenUtils;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.utils.TreeUtilities;
import org.javarosa.core.services.storage.IStorageIterator;
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



        skipCache = true;
        FormplayerSentry.captureException(new Exception("getExternalRoot skipCache: " + skipCache), SentryLevel.WARNING);
        log.info("Info getExternalRoot skipCache: " + skipCache);
        log.warn("Warn getExternalRoot skipCache: " + skipCache);
        log.error("Error getExternalRoot skipCache: " + skipCache);

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
            Collection<String> requestCaseTypes = requestData.get("case_type");
            log.info(String.format("Case search validation - requestCaseTypes: %s, requestData: %s", requestCaseTypes, requestData));
            String responseString = webClient.postFormData(url, requestData);
            if (responseString != null) {
                byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
                validateCaseTypesInResponse(responseBytes, instanceId, requestCaseTypes, url, requestData);
                ByteArrayInputStream responeStream = new ByteArrayInputStream(responseBytes);
                if (shouldParseIntoCaseSearchStorage(source.useCaseTemplate())) {
                    parseIntoCaseSearchStorage(caseSearchDb, caseSearchSandbox, caseSearchStorage, responeStream,
                            caseSearchIndexTable);
                    // Validate case types after parsing into storage
                    validateCaseTypesInStorage(caseSearchStorage, requestCaseTypes, url, requestData);
                } else {
                    TreeElement root = TreeUtilities.xmlStreamToTreeElement(responeStream, instanceId);
                    if (root != null) {
                        cache.put(cacheKey, root);
                    }
                    // Validate case types in TreeElement

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

    private void validateCaseTypesInResponse(byte[] responseBytes, String instanceId, Collection<String> requestCaseTypes,
                                             String url, Multimap<String, String> requestData) throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException,IOException {

        log.info(String.format("validateCaseTypesInResponse called - requestCaseTypes: %s, responseLength: %d", requestCaseTypes, responseBytes.length));
        
        if (requestCaseTypes == null || requestCaseTypes.isEmpty()) {
            log.warn("No case_type in request data - skipping validation");
            return;
        }
        
        ByteArrayInputStream responseStream = new ByteArrayInputStream(responseBytes);
        TreeElement root = TreeUtilities.xmlStreamToTreeElement(responseStream, instanceId);

        if (root == null) {
            log.warn("Root element is null - cannot validate");
            return;
        }
        
        log.info(String.format("Root element has %d children", root.getNumChildren()));
        
        for (int i = 0; i < root.getNumChildren(); i++) {
            TreeElement child = root.getChildAt(i);
            String attributeValue = child.getAttributeValue(null, "case_type");
            log.info(String.format("Child %d: name=%s, case_type=%s", i, child.getName(), attributeValue));

            if (attributeValue != null && !requestCaseTypes.contains(attributeValue)) {
                Exception e = new Exception("Response case type did not match request. Expected: " + requestCaseTypes.toString() + " Got: " + attributeValue + "\n" +
                        "Url: " + url + " request data: " + requestData.toString());
                FormplayerSentry.captureException(e, SentryLevel.WARNING);
                log.error("CASE TYPE MISMATCH DETECTED!", e);
            }
        }
    }

    private void validateCaseTypesInStorage(IStorageUtilityIndexed<Case> caseSearchStorage,
                                            Collection<String> requestCaseTypes, String url, Multimap<String, String> requestData) {
        log.info(String.format("validateCaseTypesInStorage called - requestCaseTypes: %s", requestCaseTypes));
        
        if (requestCaseTypes == null || requestCaseTypes.isEmpty()) {
            log.warn("No case_type in request data - skipping storage validation");
            return;
        }
        try {
            IStorageIterator<Case> iterator = caseSearchStorage.iterate();
            int caseCount = 0;
            while (iterator.hasMore()) {
                Case caseObj = iterator.nextRecord();
                caseCount++;
                String caseType = caseObj.getTypeId();
                log.info(String.format("Storage case %d: id=%s, type=%s", caseCount, caseObj.getCaseId(), caseType));
                
                if (caseType != null && !requestCaseTypes.contains(caseType)) {
                    Exception e = new Exception("Storage case type did not match request. Expected: " + requestCaseTypes.toString() + " Got: " + caseType + "\n" +
                            "Url: " + url + " request data: " + requestData.toString());
                    FormplayerSentry.captureException(e, SentryLevel.WARNING);
                    log.error("STORAGE CASE TYPE MISMATCH DETECTED!", e);
                }
            }
            log.info(String.format("Validated %d cases from storage", caseCount));
        } catch (Exception e) {
            log.error("Error validating case types from storage", e);
        }
    }

    private TreeElement getCachedRoot(Cache cache, String cacheKey, String url, boolean skipCache) {
        if (skipCache) {
            log.info("Skipping cache check for case search results");
        } else {
            TreeElement cachedRoot = cache.get(cacheKey, TreeElement.class);
            if (cachedRoot != null) {
                log.info(String.format("Using cached case search results for %s", url));
                // Deep copy to avoid concurrency issues
                TreeElement copyOfRoot = SerializationUtil.deserialize(ExtUtil.serialize(cachedRoot),
                        TreeElement.class);
                return copyOfRoot;
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
        return builder.toString();
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
