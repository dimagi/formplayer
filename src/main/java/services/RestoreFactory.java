package services;

import application.SQLiteProperties;
import auth.HqAuth;
import beans.AuthenticatedRequestBean;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.getsentry.raven.event.Breadcrumbs;
import exceptions.AsyncRetryException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.services.PropertyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.sqlite.SQLiteConnection;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sandbox.SqlSandboxUtils;
import sandbox.UserSqlSandbox;
import util.SentryUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Factory that determines the correct URL endpoint based on domain, host, and username/asUsername,
 * then retrieves and returns the restore XML.
 */
@Component
public class RestoreFactory implements ConnectionHandler{
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

    @Autowired
    private RedisTemplate redisTemplateLong;

    @Resource(name="redisTemplateLong")
    private ValueOperations<String, Long> valueOperations;

    private final Log log = LogFactory.getLog(RestoreFactory.class);

    private Connection connection;

    public void configure(AuthenticatedRequestBean authenticatedRequestBean, HqAuth auth) {
        log.info(String.format("configuring RestoreFactory with arguments " +
                "username = %s, asUsername = %s, domain = %s", username, asUsername, domain));
        this.setUsername(authenticatedRequestBean.getUsername());
        this.setDomain(authenticatedRequestBean.getDomain());
        this.setAsUsername(authenticatedRequestBean.getRestoreAs());
        this.setHqAuth(auth);
    }

    public UserSqlSandbox getSqlSandbox() {
        return new UserSqlSandbox(this);
    }

    public String getUsernameDetail() {
        if (asUsername != null) {
            return username + "_" + asUsername;
        }
        return username;
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                DataSource dataSource = SqlSandboxUtils.getDataSource("user", getDbPath());
                connection = dataSource.getConnection();
            } else {
                if (connection instanceof SQLiteConnection) {
                    SQLiteConnection sqLiteConnection = (SQLiteConnection) connection;
                    if (!sqLiteConnection.url().contains(getDbPath())) {
                        log.error(String.format("Had connection with path %s in StorageFactory %s",
                                sqLiteConnection.url(),
                                toString()));
                        DataSource dataSource = SqlSandboxUtils.getDataSource("user", getDbPath());
                        connection = dataSource.getConnection();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection = null;
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            getConnection().setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public String getDbFile() {
        return getDbPath() + "/user.db";
    }

    private String getDbPath() {
        return SQLiteProperties.getDataDir() + domain + "/" + getUsernameDetail();
    }

    public String getWrappedUsername() {
        return asUsername == null ? username : asUsername;
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

    public InputStream getRestoreXml() {
        return getRestoreXml(false);
    }

    public InputStream getRestoreXml(boolean overwriteCache) {
        ensureValidParameters();

        String restoreUrl;
        if (asUsername == null) {
            restoreUrl = getRestoreUrl(host, domain, overwriteCache);
        } else {
            restoreUrl = getRestoreUrl(host, domain, asUsername, overwriteCache);
        }

        Map<String, String> data = new HashMap<String, String>();
        data.put("restoreUrl", restoreUrl);

        BreadcrumbBuilder builder = new BreadcrumbBuilder();
        builder.setData(data);
        builder.setCategory("restore");
        builder.setMessage("Restoring from URL " + restoreUrl);
        SentryUtils.recordBreadcrumb(builder.build());

        log.info("Restoring from URL " + restoreUrl);
        InputStream restoreStream = getRestoreXmlHelper(restoreUrl, hqAuth);
        setLastSyncTime();
        return restoreStream;
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

    private InputStream getRestoreXmlHelper(String restoreUrl, HqAuth auth) {
        RestTemplate restTemplate = new RestTemplate();
        log.info("Restoring at domain: " + domain + " with auth: " + auth);
        HttpHeaders headers = auth.getAuthHeaders();
        headers.add("x-openrosa-version", "2.0");
        ResponseEntity<org.springframework.core.io.Resource> response = restTemplate.exchange(
                restoreUrl,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                org.springframework.core.io.Resource.class
        );

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
        } catch (IOException e) {
            throw new RuntimeException("Unable to read restore response", e);
        }
        return stream;
    }

    public static String getRestoreUrl(String host, String domain, boolean overwriteCache){
        String url = host + "/a/" + domain + "/phone/restore/?version=2.0";
        if (overwriteCache) {
            url += "&overwrite_cache=true";
        }
        return url;
    }

    public String getRestoreUrl(String host, String domain, String username, boolean overwriteCache) {
        String url = host + "/a/" + domain + "/phone/restore/?as=" + username + "@" +
                domain + ".commcarehq.org&version=2.0";

        if (overwriteCache) {
            url += "&overwrite_cache=true";
        }
        return url;
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
}
