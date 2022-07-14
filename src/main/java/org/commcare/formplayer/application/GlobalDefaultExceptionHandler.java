package org.commcare.formplayer.application;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.catalina.connector.ClientAbortException;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.formplayer.aspects.LockAspect;
import org.commcare.formplayer.beans.exceptions.ExceptionResponseBean;
import org.commcare.formplayer.beans.exceptions.HTMLExceptionResponseBean;
import org.commcare.formplayer.beans.exceptions.RetryExceptionResponseBean;
import org.commcare.formplayer.exceptions.*;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.FormplayerSentry;
import org.commcare.modern.models.RecordTooLargeException;
import org.commcare.util.screen.CommCareSessionException;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@CommonsLog
public class GlobalDefaultExceptionHandler {

    @Autowired
    private FormplayerDatadog datadog;

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
    public ExceptionResponseBean handleApplicationError(HttpServletRequest request, Exception exception) {
        log.info("Application error", exception);
        datadog.incrementErrorCounter(Constants.DATADOG_ERRORS_APP_CONFIG, request);
        return getPrettyExceptionResponse(exception, request);
    }

    private ExceptionResponseBean getPrettyExceptionResponse(Exception exception, HttpServletRequest request) {
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
    public ExceptionResponseBean handleHttpRequestError(HttpServletRequest req, HttpClientErrorException exception) {
        datadog.incrementErrorCounter(Constants.DATADOG_ERRORS_EXTERNAL_REQUEST, req);
        log.error(String.format("Exception %s making external request %s.", exception, req), exception);
        return new ExceptionResponseBean(exception.getResponseBodyAsString(), req.getRequestURL().toString());
    }

    @ExceptionHandler({AsyncRetryException.class})
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @ResponseBody
    public RetryExceptionResponseBean handleAsyncRetryException(HttpServletRequest req, AsyncRetryException exception) {
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
    public HTMLExceptionResponseBean handleFormattedApplicationError(HttpServletRequest req, Exception exception) {
        log.info("Application configuration error", exception);
        datadog.incrementErrorCounter(Constants.DATADOG_ERRORS_APP_CONFIG, req);
        return new HTMLExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    @ExceptionHandler({LockAspect.LockError.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.LOCKED)
    public ExceptionResponseBean handleLockError(HttpServletRequest req, Exception exception) {
        log.info("User lock timed out", exception);
        return new ExceptionResponseBean("User lock timed out", req.getRequestURL().toString());
    }

    @ExceptionHandler({InterruptedRuntimeException.class})
    @ResponseBody
    public ExceptionResponseBean handleInterruptException(HttpServletRequest req, Exception exception) {
        return new ExceptionResponseBean("An issue prevented us from processing your previous action, please try again",
                req.getRequestURL().toString());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ExceptionResponseBean handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception.getClass(), exception);
        datadog.incrementErrorCounter(Constants.DATADOG_ERRORS_CRASH, req);
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
}
