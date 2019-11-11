package services;

import api.process.FormRecordProcessorHelper;
import auth.HqAuth;
import beans.AuthenticatedRequestBean;
import com.timgroup.statsd.StatsDClient;
import engine.FormplayerTransactionParserFactory;
import exceptions.AsyncRetryException;
import exceptions.SQLiteRuntimeException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.util.Pair;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import sandbox.JdbcSqlStorageIterator;
import sandbox.UserSqlSandbox;
import sqlitedb.SQLiteDB;
import sqlitedb.UserDB;
import util.Constants;
import util.FormplayerHttpRequest;
import util.FormplayerSentry;
import util.RequestUtils;
import util.SimpleTimer;
import util.UserUtils;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Factory that determines the correct URL endpoint based on domain, host, and username/asUsername,
 * then retrieves and returns the restore XML.
 */
@Component
public class RestoreFactory {
    @Value("${commcarehq.host}")
    private String host;

    private String asUsername;
    private String username;
    private String domain;
    private HqAuth hqAuth;

    public static final String FREQ_DAILY = "freq-daily";
    public static final String FREQ_WEEKLY = "freq-weekly";
    public static final String FREQ_NEVER = "freq-never";

    public static final Long ONE_DAY_IN_MILLISECONDS = 86400000l;
    public static final Long ONE_WEEK_IN_MILLISECONDS = ONE_DAY_IN_MILLISECONDS * 7;

    private static final String DEVICE_ID_SLUG = "WebAppsLogin";

    @Autowired(required = false)
    private FormplayerHttpRequest request;

    @Autowired
    private FormplayerSentry raven;

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    private RedisTemplate redisTemplateLong;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Resource(name="redisTemplateLong")
    private ValueOperations<String, Long> valueOperations;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    private final Log log = LogFactory.getLog(RestoreFactory.class);

    CategoryTimingHelper.RecordingTimer downloadRestoreTimer;

    private SQLiteDB sqLiteDB = new SQLiteDB(null);
    private boolean useLiveQuery;
    private boolean hasRestored;
    private String caseId;
    private boolean configured = false;

    public void configure(String domain, String caseId, HqAuth auth) {
        this.setUsername(UserUtils.getRestoreAsCaseIdUsername(caseId));
        this.setDomain(domain);
        this.setCaseId(caseId);
        this.setHqAuth(auth);
        this.hasRestored = false;
        this.configured = true;
        sqLiteDB = new UserDB(domain, username, null);
        log.info(String.format("configuring RestoreFactory with CaseID with arguments " +
                "username = %s, caseId = %s, domain = %s", username, caseId, domain));
    }

    public void configure(String username, String domain, String asUsername, HqAuth auth) {
        this.setUsername(username);
        this.setDomain(domain);
        this.setAsUsername(asUsername);
        this.hqAuth = auth;
        this.hasRestored = false;
        this.configured = true;
        sqLiteDB = new UserDB(domain, username, asUsername);
        log.info(String.format("configuring RestoreFactory with arguments " +
                "username = %s, asUsername = %s, domain = %s, useLiveQuery = %s", username, asUsername, domain, useLiveQuery));
    }

    public void configure(AuthenticatedRequestBean authenticatedRequestBean, HqAuth auth, boolean useLiveQuery) {
        this.setUsername(authenticatedRequestBean.getUsername());
        this.setDomain(authenticatedRequestBean.getDomain());
        this.setAsUsername(authenticatedRequestBean.getRestoreAs());
        this.setHqAuth(auth);
        this.setUseLiveQuery(useLiveQuery);
        this.hasRestored = false;
        this.configured = true;
        sqLiteDB = new UserDB(domain, username, asUsername);
        log.info(String.format("configuring RestoreFactory with arguments " +
                "username = %s, asUsername = %s, domain = %s, useLiveQuery = %s",
                username, asUsername, domain, useLiveQuery));
    }

    // This function will only wipe user DBs when they have expired, otherwise will incremental sync
    public UserSqlSandbox performTimedSync() throws Exception {
        return performTimedSync(true);
    }
    public UserSqlSandbox performTimedSync(boolean shouldPurge) throws Exception {
        // Create parent dirs if needed
        if(getSqlSandbox().getLoggedInUser() != null){
            getSQLiteDB().createDatabaseFolder();
        }
        UserSqlSandbox sandbox;
        try {
             sandbox = restoreUser();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 412) {
                getSQLiteDB().deleteDatabaseFile();
                // this line has the effect of clearing the sync token
                // from the restore URL that's used
                sqLiteDB = new UserDB(domain, username, asUsername);
                return performTimedSync(shouldPurge);
            }
            throw e;
        }
        if (shouldPurge && sandbox != null) {
            SimpleTimer purgeTimer = new SimpleTimer();
            purgeTimer.start();
            FormRecordProcessorHelper.purgeCases(sandbox);
            purgeTimer.end();
            categoryTimingHelper.recordCategoryTiming(purgeTimer, Constants.TimingCategories.PURGE_CASES);
        }
        return sandbox;
    }

    // This function will attempt to get the user DBs without syncing if they exist, sync if not
    public UserSqlSandbox getSandbox() throws Exception {
        if(getSqlSandbox().getLoggedInUser() != null
                && !isRestoreXmlExpired()){
            return getSqlSandbox();
        } else {
            getSQLiteDB().createDatabaseFolder();
            return restoreUser();
        }
    }

    private UserSqlSandbox restoreUser() throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        int maxRetries = 2;
        int counter = 0;
        while (true) {
            try {
                UserSqlSandbox sandbox = getSqlSandbox();
                SimpleTimer parseTimer = new SimpleTimer();
                parseTimer.start();
                FormplayerTransactionParserFactory factory = new FormplayerTransactionParserFactory(sandbox, true);
                InputStream restoreStream = getRestoreXml();
                setAutoCommit(false);
                ParseUtils.parseIntoSandbox(restoreStream, factory, true, true);
                hasRestored = true;
                commit();
                setAutoCommit(true);
                parseTimer.end();
                categoryTimingHelper.recordCategoryTiming(parseTimer, Constants.TimingCategories.PARSE_RESTORE);
                sandbox.writeSyncToken();
                return sandbox;
            } catch (InvalidStructureException | SQLiteRuntimeException e) {
                if (e instanceof InvalidStructureException || ++counter >= maxRetries) {
                    // Before throwing exception, rollback any changes to relinquish SQLite lock
                    rollback();
                    setAutoCommit(true);
                    getSQLiteDB().deleteDatabaseFile();
                    getSQLiteDB().createDatabaseFolder();
                    throw e;
                } else {
                    log.info(String.format("Retrying restore for user %s after receiving exception.",
                            getEffectiveUsername()),
                            e);
                }
            }
        }
    }

    public UserSqlSandbox getSqlSandbox() {
        return new UserSqlSandbox(this.sqLiteDB);
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            sqLiteDB.getConnection().setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean getAutoCommit() {
        try {
            return sqLiteDB.getConnection().getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            sqLiteDB.getConnection().commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            sqLiteDB.getConnection().rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SQLiteDB getSQLiteDB() {
        return sqLiteDB;
    }

    public String getWrappedUsername() {
        return asUsername == null ? username : asUsername;
    }

    public String getEffectiveUsername() {
        return UserUtils.getShortUsername(getWrappedUsername(), domain);
    }

    private void ensureValidParameters() {
        if (domain == null || (username == null && asUsername == null)) {
            throw new RuntimeException("Domain and one of username or asUsername must be non-null. " +
                    " Domain: " + domain +
                    ", username: " + username +
                    ", asUsername: " + asUsername);
        }
    }

    public String getSyncFreqency() {
        try {
            return storageFactory.getPropertyManager().getSingularProperty("cc-autosync-freq");
        } catch (RuntimeException e) {
            // In cases where we don't have access to the PropertyManager, such sync-db, this call
            // throws a RuntimeException
            return RestoreFactory.FREQ_NEVER;
        }
    }

    /**
     * Based on the frequency of restore set in the app, this method determines
     * whether the user should sync
     *
     * @return boolean - true if restore has expired, false otherwise
     */
    public boolean isRestoreXmlExpired() {
        String freq = getSyncFreqency();
        Long lastSyncTime = getLastSyncTime();
        if (lastSyncTime == null || freq == null) {
            return false;
        }
        Long delta = System.currentTimeMillis() - lastSyncTime;

        switch (freq) {
            case FREQ_DAILY:
                return delta > ONE_DAY_IN_MILLISECONDS;
            case FREQ_WEEKLY:
                return delta > ONE_WEEK_IN_MILLISECONDS;
            case FREQ_NEVER:
                return false;
            default:
                return false;
        }
    }

    public InputStream getRestoreXml() {
        ensureValidParameters();
        Pair<String, HttpHeaders> restoreUrlAndHeaders = getRestoreUrlAndHeaders();
        recordSentryData(restoreUrlAndHeaders.first);
        log.info("Restoring from URL " + restoreUrlAndHeaders.first);
        InputStream restoreStream = getRestoreXmlHelper(restoreUrlAndHeaders.first, restoreUrlAndHeaders.second);
        setLastSyncTime();
        return restoreStream;
    }

    private void recordSentryData(final String restoreUrl) {
        raven.newBreadcrumb()
                .setData("restoreUrl", restoreUrl)
                .setCategory("restore")
                .setMessage("Restoring from URL " + restoreUrl)
                .record();
    }

    private void setLastSyncTime() {
        valueOperations.set(lastSyncKey(), System.currentTimeMillis(), 10, TimeUnit.DAYS);
    }

    public Long getLastSyncTime() {
        // valueOperations should only be null when we don't have access to Redis.
        // This currently only happens in tests.
        if (valueOperations == null) {
            return null;
        }
        return valueOperations.get(lastSyncKey());
    }

    private String lastSyncKey() {
        return "last-sync-time:" + domain + ":" + username + ":" + asUsername;
    }

    /**
     * Given an async restore xml response, this function throws an AsyncRetryException
     * with meta data about the async restore.
     *
     * @param xml - Async restore response
     * @param headers - HttpHeaders from the restore response
     */
    private void handleAsyncRestoreResponse(String xml, HttpHeaders headers) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        ByteArrayInputStream input;
        Document doc;

        // Create the XML Document builder
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Unable to instantiate document builder");
        }

        // Parse the xml into a utf-8 byte array
        try {
            input = new ByteArrayInputStream(xml.getBytes("utf-8") );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to parse async restore response.");
        }

        // Build an XML document
        try {
            doc = builder.parse(input);
        } catch (SAXException e) {
            throw new RuntimeException("Unable to parse into XML Document");
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse into XML Document");
        }

        NodeList messageNodes = doc.getElementsByTagName("message");
        NodeList progressNodes = doc.getElementsByTagName("progress");

        assert messageNodes.getLength() == 1;
        assert progressNodes.getLength() == 1;

        String message = messageNodes.item(0).getTextContent();
        Node progressNode = progressNodes.item(0);
        NamedNodeMap attributes = progressNode.getAttributes();

        throw new AsyncRetryException(
                message,
                Integer.parseInt(attributes.getNamedItem("done").getTextContent()),
                Integer.parseInt(attributes.getNamedItem("total").getTextContent()),
                Integer.parseInt(headers.get("retry-after").get(0))
        );
    }

    private InputStream getRestoreXmlHelper(String restoreUrl, HttpHeaders headers) {
        ResponseEntity<org.springframework.core.io.Resource> response;
        String status = "error";
        log.info("Restoring at domain: " + domain + " with url: " + restoreUrl);
        downloadRestoreTimer = categoryTimingHelper.newTimer(Constants.TimingCategories.DOWNLOAD_RESTORE);
        downloadRestoreTimer.start();
        try {
            response = restTemplateBuilder.build().exchange(
                    restoreUrl,
                    HttpMethod.GET,
                    new HttpEntity<String>(headers),
                    org.springframework.core.io.Resource.class
            );
            status = response.getStatusCode().toString();
        } catch (HttpClientErrorException e) {
            status = e.getStatusCode().toString();
        } finally {
            downloadRestoreTimer.end();
            datadogStatsDClient.increment(
                    Constants.DATADOG_RESTORE_COUNT,
                    "domain:" + domain,
                    "duration:" + downloadRestoreTimer.getDurationBucket(),
                    "status:" + status
            );
        }

        // Handle Async restore
        if (response.getStatusCode().value() == 202) {
            String responseBody = null;
            try {
                responseBody = IOUtils.toString(response.getBody().getInputStream(), "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Unable to read async restore response", e);
            }
            handleAsyncRestoreResponse(responseBody, response.getHeaders());
        }

        InputStream stream = null;
        try {
            stream = response.getBody().getInputStream();
            downloadRestoreTimer.record();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read restore response", e);
        }
        return stream;
    }

    public String getSyncToken() {
        JdbcSqlStorageIterator<User> iterator = getSqlSandbox().getUserStorage().iterate();
        try {
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next().getLastSyncToken();
        } finally {
            iterator.close();
        }
    }

    // Device ID for tracking usage in the same way Android uses IMEI
    private String getSyncDeviceId() {
        if (asUsername == null) {
            return DEVICE_ID_SLUG;
        }
        return String.format("%s*%s*as*%s", DEVICE_ID_SLUG, username, asUsername);
    }

    public HttpHeaders getUserHeaders() {
        if (getHqAuth() == null) {
            throw new RuntimeException("Trying to get Authentication headers but request " +
                    " did not have an authentication key.");
        }
        HttpHeaders headers = getHqAuth().getAuthHeaders();
        headers.set("X-CommCareHQ-LastSyncToken", getSyncToken());
        headers.set("X-OpenRosa-Version", "3.0");
        headers.set("X-OpenRosa-DeviceId", getSyncDeviceId());
        return headers;
    }

    public Pair<String, HttpHeaders> getRestoreUrlAndHeaders() {
        if (caseId != null) {
            return getCaseRestoreUrlAndHeaders();
        }
        return getUserRestoreUrlAndHeaders();
    }

    private HttpHeaders getHmacHeaders(String requestPath) {
        if (!request.getRequestValidatedWithHMAC()) {
            throw new RuntimeException(String.format("Tried getting HMAC Auth for request %s but this request" +
                    "was not validated with HMAC.", requestPath));
        }
        HttpHeaders headers = new HttpHeaders(){
            {
                add("x-openrosa-version", "2.0");
            }
        };
        try {
            String digest = RequestUtils.getHmac(formplayerAuthKey, requestPath);
            headers.add("X-MAC-DIGEST", digest);
            return headers;
        } catch (Exception e) {
            log.error("Could not get HMAC signature to auth restore request", e);
            throw new RuntimeException(e);
        }
    }

    public Pair<String, HttpHeaders> getCaseRestoreUrlAndHeaders() {
        StringBuilder builder = new StringBuilder();
        builder.append("/a/");
        builder.append(domain);
        builder.append("/case_migrations/restore/");
        builder.append(caseId);
        builder.append("/");
        HttpHeaders headers = getHmacHeaders(builder.toString());
        String fullUrl = host + builder.toString();
        return new Pair<> (fullUrl, headers);
    }

    public Pair<String, HttpHeaders> getUserRestoreUrlAndHeaders() {
        StringBuilder builder = new StringBuilder();
        builder.append("/a/");
        builder.append(domain);
        builder.append("/phone/restore/?version=2.0");
        String syncToken = getSyncToken();
        if (syncToken != null && !"".equals(syncToken)) {
            builder.append("&since=").append(syncToken);
        }
        builder.append("&device_id=").append(getSyncDeviceId());

        if (useLiveQuery) {
            builder.append("&case_sync=livequery");
        }

        if( asUsername != null) {
            builder.append("&as=").append(asUsername).append("@").append(domain).append(".commcarehq.org");
        }
        String restoreUrl = builder.toString();
        HttpHeaders headers;
        if (getHqAuth() == null) {
            // Need to do HMAC auth
            headers = getHmacHeaders(restoreUrl);
        } else {
            headers = getHqAuth().getAuthHeaders();
            headers.add("x-openrosa-version", "2.0");
        }
        String fullUrl = host + restoreUrl;
        return new Pair<>(fullUrl, headers);
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = TableBuilder.scrubName(username);
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public HqAuth getHqAuth() {
        return hqAuth;
    }

    public void setHqAuth(HqAuth hqAuth) {
        this.hqAuth = hqAuth;
    }

    public String getAsUsername() {
        return asUsername;
    }

    public void setAsUsername(String asUsername) {
        this.asUsername = asUsername;
    }

    public boolean isUseLiveQuery() {
        return useLiveQuery;
    }

    public void setUseLiveQuery(boolean useLiveQuery) {
        this.useLiveQuery = useLiveQuery;
    }

    public boolean getHasRestored() {
        return hasRestored;
    }

    public SimpleTimer getDownloadRestoreTimer() {
        return downloadRestoreTimer;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getCaseId() {
        return caseId;
    }

    public boolean isConfigured() { return this.configured; }
}
