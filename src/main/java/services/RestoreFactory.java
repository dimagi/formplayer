package services;

import api.process.FormRecordProcessorHelper;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.AuthenticatedRequestBean;
import engine.FormplayerTransactionParserFactory;
import exceptions.AsyncRetryException;
import exceptions.SQLiteRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import sandbox.AbstractSqlIterator;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import sqlitedb.SQLiteDB;
import sqlitedb.UserDB;
import util.*;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    private FormplayerSentry raven;

    @Autowired
    private RedisTemplate redisTemplateLong;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Resource(name="redisTemplateLong")
    private ValueOperations<String, Long> valueOperations;

    private final Log log = LogFactory.getLog(RestoreFactory.class);

    private SQLiteDB sqLiteDB = new SQLiteDB(null);
    private boolean useLiveQuery;

    public void configure(AuthenticatedRequestBean authenticatedRequestBean, HqAuth auth, boolean useLiveQuery) {
        this.setUsername(authenticatedRequestBean.getUsername());
        this.setDomain(authenticatedRequestBean.getDomain());
        this.setAsUsername(authenticatedRequestBean.getRestoreAs());
        this.setHqAuth(auth);
        this.setUseLiveQuery(useLiveQuery);
        sqLiteDB = new UserDB(domain, username, asUsername);
        log.info(String.format("configuring RestoreFactory with arguments " +
                "username = %s, asUsername = %s, domain = %s, useLiveQuery = %s", username, asUsername, domain, useLiveQuery));
    }

    // This function will only wipe user DBs when they have expired, otherwise will incremental sync
    public UserSqlSandbox performTimedSync() throws Exception {
        // Create parent dirs if needed
        if(getSqlSandbox().getLoggedInUser() != null){
            getSQLiteDB().createDatabaseFolder();
        }
        UserSqlSandbox sandbox = restoreUser();
        SimpleTimer purgeTimer = new SimpleTimer();
        purgeTimer.start();
        FormRecordProcessorHelper.purgeCases(sandbox);
        purgeTimer.end();
        categoryTimingHelper.recordCategoryTiming(purgeTimer, Constants.TimingCategories.PURGE_CASES);
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
                SimpleTimer parseTimer = new SimpleTimer();
                parseTimer.start();
                setAutoCommit(false);

                performRestore();
                commit();
                setAutoCommit(true);

                parseTimer.end();
                categoryTimingHelper.recordCategoryTiming(parseTimer, Constants.TimingCategories.PARSE_RESTORE);
                return getSqlSandbox();
            } catch (SQLiteRuntimeException e) {
                if (++counter >= maxRetries) {
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
            } finally {
                setAutoCommit(true);
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
            return (String) PropertyManager.instance().getProperty("cc-autosync-freq").get(0);
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
        if (lastSyncTime == null) {
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

    public void performRestore() {
        ensureValidParameters();
        String restoreUrl = getRestoreUrl();
        recordSentryData(restoreUrl);
        log.info("Restoring from URL " + restoreUrl);
        UserSqlSandbox sandbox = getSqlSandbox();
        FormplayerTransactionParserFactory factory = new FormplayerTransactionParserFactory(sandbox, true);
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new RestoreHttpMessageConverter(factory));
        RestTemplate restTemplate = new RestTemplate(converters);
        log.info("Restoring at domain: " + domain + " with auth: " + hqAuth + " with url: " + restoreUrl);
        HttpHeaders headers = hqAuth.getAuthHeaders();
        headers.add("x-openrosa-version", "2.0");
        SimpleTimer timer = new SimpleTimer();
        timer.start();
        restTemplate.exchange(
                restoreUrl,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                Object.class
        );
        timer.end();
        setLastSyncTime();
        sandbox.writeSyncToken();
        categoryTimingHelper.recordCategoryTiming(timer, Constants.TimingCategories.DOWNLOAD_RESTORE);
    }

    public String getSyncToken() {
        SqliteIndexedStorageUtility<User> storage = getSqlSandbox().getUserStorage();
        AbstractSqlIterator<User> iterator = storage.iterate();
        //should be exactly one user
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next().getLastSyncToken();
    }

    // Device ID for tracking usage in the same way Android uses IMEI
    private String getSyncDeviceId() {
        if (asUsername == null) {
            return DEVICE_ID_SLUG;
        }
        return String.format("%s*%s*as*%s", DEVICE_ID_SLUG, username, asUsername);
    }

    public HttpHeaders getUserHeaders() {
        HttpHeaders headers = getHqAuth().getAuthHeaders();
        headers.set("X-CommCareHQ-LastSyncToken", getSyncToken());
        headers.set("X-OpenRosa-Version", "3.0");
        headers.set("X-OpenRosa-DeviceId", getSyncDeviceId());
        return headers;
    }

    public String getRestoreUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append(host);
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

        if (asUsername != null) {
            builder.append("&as=").append(asUsername).append("@").append(domain).append(".commcarehq.org");
        }

        return builder.toString();
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
}
