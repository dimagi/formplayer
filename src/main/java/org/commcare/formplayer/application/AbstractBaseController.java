package org.commcare.formplayer.application;

import com.timgroup.statsd.StatsDClient;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.formplayer.aspects.LockAspect;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.exceptions.ExceptionResponseBean;
import org.commcare.formplayer.beans.exceptions.HTMLExceptionResponseBean;
import org.commcare.formplayer.beans.exceptions.RetryExceptionResponseBean;
import org.commcare.formplayer.exceptions.*;
import org.commcare.formplayer.repo.MenuSessionRepo;
import org.commcare.formplayer.services.*;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerHttpRequest;
import org.commcare.formplayer.util.FormplayerSentry;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.models.RecordTooLargeException;
import org.commcare.util.screen.CommCareSessionException;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

/**
 * Base Controller class containing exception handling logic and
 * autowired beans used in both MenuController and FormController
 */
public abstract class AbstractBaseController {

//    @Autowired
//    protected CaseSearchHelper caseSearchHelper;

    private WebClient webClient;

    @Autowired
    protected FormSessionService formSessionService;

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

    @Autowired
    protected InstallService installService;

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    protected NewFormResponseFactory newFormResponseFactory;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    protected FormSendCalloutHandler formSendCalloutHandler;

    @Autowired
    protected MenuSessionFactory menuSessionFactory;

    @Autowired
    protected MenuSessionRunnerService runnerService;

    private final Log log = LogFactory.getLog(AbstractBaseController.class);

    /**
     * Catch all the exceptions that we *do not* want emailed here
     */
    @ExceptionHandler({ApplicationConfigException.class,
            XPathException.class,
            CommCareInstanceInitializer.FixtureInitializationException.class,
            CommCareSessionException.class,
            FormNotFoundException.class,
            RecordTooLargeException.class,
            InvalidStructureException.class,
            UnresolvedResourceRuntimeException.class,
            NoLocalizedTextException.class})
    @ResponseBody
    public ExceptionResponseBean handleApplicationError(FormplayerHttpRequest request, Exception exception) {
        log.error("Request: " + request.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_APP_CONFIG, request);
        FormplayerSentry.captureException(exception, SentryLevel.INFO);
        return getPrettyExceptionResponse(exception, request);
    }

    private ExceptionResponseBean getPrettyExceptionResponse(Exception exception, FormplayerHttpRequest request) {
        String message = exception.getMessage();
        if (exception instanceof XPathTypeMismatchException && message.contains("instance(groups)")) {
            message = "The case sharing settings for your user are incorrect. " +
                    "This user must be in exactly one case sharing group. " +
                    "Please contact your supervisor.";
        }
        return new ExceptionResponseBean(message, request.getRequestURL().toString());
    }

    /**
     * Handles exceptions thrown when making external requests, usually to CommCareHQ.
     */
    @ExceptionHandler({HttpClientErrorException.class})
    @ResponseBody
    public ExceptionResponseBean handleHttpRequestError(FormplayerHttpRequest req, HttpClientErrorException exception) {
        incrementDatadogCounter(Constants.DATADOG_ERRORS_EXTERNAL_REQUEST, req);
        log.error(String.format("Exception %s making external request %s.", exception, req));
        Sentry.captureException(exception);
        return new ExceptionResponseBean(exception.getResponseBodyAsString(), req.getRequestURL().toString());
    }

    @ExceptionHandler({AsyncRetryException.class})
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @ResponseBody
    public RetryExceptionResponseBean handleAsyncRetryException(FormplayerHttpRequest req, AsyncRetryException exception) {
        return new RetryExceptionResponseBean(
                exception.getMessage(),
                req.getRequestURL().toString(),
                exception.getDone(),
                exception.getTotal(),
                exception.getRetryAfter()
        );
    }

    /**
     * Catch exceptions that have formatted HTML errors
     */
    @ExceptionHandler({FormattedApplicationConfigException.class})
    @ResponseBody
    public HTMLExceptionResponseBean handleFormattedApplicationError(FormplayerHttpRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_APP_CONFIG, req);
        FormplayerSentry.captureException(exception, SentryLevel.INFO);
        return new HTMLExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    @ExceptionHandler({LockAspect.LockError.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.LOCKED)
    public ExceptionResponseBean handleLockError(FormplayerHttpRequest req, Exception exception) {
        FormplayerSentry.captureException(exception, SentryLevel.INFO);
        return new ExceptionResponseBean("User lock timed out", req.getRequestURL().toString());
    }

    @ExceptionHandler({InterruptedRuntimeException.class})
    @ResponseBody
    public ExceptionResponseBean handleInterruptException(FormplayerHttpRequest req, Exception exception) {
        return new ExceptionResponseBean("An issue prevented us from processing your previous action, please try again",
                req.getRequestURL().toString());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ExceptionResponseBean handleError(FormplayerHttpRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_CRASH, req);
        exception.printStackTrace();
        Sentry.captureException(exception);
        String message = exception.getMessage();
        if (exception instanceof ClientAbortException) {
            // We can't actually return anything since the client has bailed. To avoid errors return null
            // https://mtyurt.net/2016/04/18/spring-how-to-handle-ioexception-broken-pipe/
            log.error("Client Aborted! Returning null");
            return null;
        } else if (exception instanceof DataAccessException) {
            message = "An issue prevented us from processing your action, please try again";
        }
        return new ExceptionResponseBean(message, req.getRequestURL().toString());
    }

    void logNotification(@Nullable NotificationMessage notification, HttpServletRequest req) {
        try {
            if (notification != null && notification.getType() == NotificationMessage.Type.error.name()) {
                Sentry.captureException(new RuntimeException(notification.getMessage()));
                incrementDatadogCounter(Constants.DATADOG_ERRORS_NOTIFICATIONS, req, notification.getTag());
            } else if (notification != null && notification.getType() == NotificationMessage.Type.app_error.name()) {
                FormplayerSentry.captureException(new ApplicationConfigException(notification.getMessage()), SentryLevel.INFO);
                incrementDatadogCounter(Constants.DATADOG_ERRORS_APP_CONFIG, req, notification.getTag());
            }
        } catch (Exception e) {
            // we don't wanna crash while logging the error
        }
    }

    private void incrementDatadogCounter(String metric, FormplayerHttpRequest req) {
        incrementDatadogCounter(metric, req, null);
    }

    private void incrementDatadogCounter(String metric, HttpServletRequest req, String tag) {
        String user = "unknown";
        String domain = "unknown";
        if(req instanceof FormplayerHttpRequest) {
            FormplayerHttpRequest formplayerRequest = ((FormplayerHttpRequest)req);
            if (formplayerRequest.getUserDetails() != null) {
                user = formplayerRequest.getUserDetails().getUsername();
            }
            if (formplayerRequest.getDomain() != null) {
                domain = formplayerRequest.getDomain();
            }
        }
        ArrayList<String> tags = new ArrayList<>();
        tags.add("domain:" + domain);
        tags.add("user:" + user);
        tags.add("request:" + req.getRequestURI());
        if (tag != null) {
            tags.add("tag:" + tag);
        }

        datadogStatsDClient.increment(
                metric,
                tags.toArray(new String[tags.size()])
        );
    }

    protected MenuSession getMenuSessionFromBean(SessionNavigationBean sessionNavigationBean) throws Exception {
        MenuSession menuSession = performInstall(sessionNavigationBean);
        menuSession.setCurrentBrowserLocation(sessionNavigationBean.getGeoLocation());
        return menuSession;
    }

    protected MenuSession performInstall(InstallRequestBean bean) throws Exception {
        if (bean.getAppId() == null || bean.getAppId().isEmpty()) {
            throw new RuntimeException("App_id must not be null.");
        }

        return menuSessionFactory.buildSession(
                bean.getUsername(),
                bean.getDomain(),
                bean.getAppId(),
                bean.getLocale(),
                bean.getOneQuestionPerScreen(),
                bean.getRestoreAs(),
                bean.getPreview()
        );
    }

}
