package util;

import com.getsentry.raven.Raven;
import com.getsentry.raven.event.Breadcrumb;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.User;
import com.getsentry.raven.event.interfaces.ExceptionInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.URISyntaxException;

/**
 * Created by benrudolph on 4/27/17.
 */
public class FormplayerRaven {

    private static final Log log = LogFactory.getLog(FormplayerRaven.class);

    private final String HQ_HOST_TAG = "HQHost";
    private final String DOMAIN_TAG = "domain";
    private final String APP_URL_EXTRA = "app_url";
    private final String APP_DOWNLOAD_URL_EXTRA = "app_download";

    private Raven raven;

    @Value("${commcarehq.environment}")
    private String environment;

    private String domain = "UNKNOWN";
    private String appId = "UNKNOWN";

    @Value("${commcarehq.host}")
    private String host;

    public FormplayerRaven(Raven raven) {
        this.raven = raven;
    }

    public void recordBreadcrumb(Breadcrumb breadcrumb) {
        if (raven == null) {
            return;
        }
        try {
            raven.getContext().recordBreadcrumb(breadcrumb);
        } catch (Exception e) {
            log.info("Error recording breadcrumb. Ensure that raven is configured. ", e);
        }
    }

    public void setUserContext(String userId, String username, String ipAddress) {
        User user = new User(
                userId,
                username,
                ipAddress,
                null
        );
        try {
            raven.getContext().setUser(user);
        } catch (Exception e) {
            log.info("Error setting user context. Ensure that raven is configured. ", e);
        }
    }

    private String getAppURL() {
        if (domain == null || appId == null) {
            return null;
        }
        return host + "/a/" + domain + "/apps/view/" + appId + "/";
    }

    private String getAppDownloadURL() {
        if (domain == null || appId == null) {
            return null;
        }
        String baseURL = host + "/a/" + domain + "/apps/api/download_ccz/";
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            log.info("Unable to build app download URL");
            return null;
        }
        builder.addParameter("app_id", appId);
        builder.addParameter("latest", Constants.CCZ_LATEST_SAVED);
        return builder.toString();
    }

    private EventBuilder getDefaultBuilder() {
        return (
                new EventBuilder()
                .withEnvironment(environment)
                .withTag(HQ_HOST_TAG, host)
                .withTag(DOMAIN_TAG, domain)
                .withExtra(APP_DOWNLOAD_URL_EXTRA, getAppDownloadURL())
                .withExtra(APP_URL_EXTRA, getAppURL())
        );
    }

    public void sendRavenException(FormplayerHttpRequest request, Exception exception) {
        sendRavenException(request, exception, Event.Level.ERROR);
    }

    public void sendRavenException(FormplayerHttpRequest request, Exception exception, Event.Level level) {
        setDomain(request.getDomain());

        if (request.getCouchUser() != null && request.getPostgresUser() != null) {
            setUserContext(
                    String.valueOf(request.getPostgresUser().getUserId()),
                    request.getCouchUser().getUsername(),
                    request.getRemoteAddr()
            );
        }

        EventBuilder eventBuilder = getDefaultBuilder()
                .withMessage(exception.getMessage())
                .withLevel(level)
                .withSentryInterface(new ExceptionInterface(exception));

        sendRavenEvent(eventBuilder);
    }

    private void sendRavenEvent(EventBuilder event) {
        if (raven == null) {
            return;
        }
        try {
            raven.sendEvent(event);
        } catch (Exception e) {
            log.info("Error sending event to Sentry. Ensure that raven is configured. ", e);
        }
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
