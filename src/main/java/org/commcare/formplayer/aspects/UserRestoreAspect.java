package org.commcare.formplayer.aspects;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import io.sentry.Sentry;
import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.auth.HqAuth;
import org.commcare.formplayer.auth.PublicFormSessionAuth;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.util.RequestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.RestoreFactory;

import java.util.Arrays;
import java.util.Optional;

import datadog.trace.api.interceptor.MutableSpan;

/**
 * Aspect to configure the RestoreFactory
 */
@Aspect
@Order(5)
public class UserRestoreAspect {

    private final Log log = LogFactory.getLog(UserRestoreAspect.class);

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    private FormSessionService formSessionService;

    @Autowired
    CategoryTimingHelper categoryTimingHelper;

    @Value("${touchforms.username:#{null}}")
    private String touchformsUsername;

    @Value("${touchforms.password:#{null}}")
    private String touchformsPassword;

    @Value("${commcarehq.formplayerAuthKey:#{null}}")
    private String authKey;

    @Before(value = "@annotation(org.commcare.formplayer.annotations.UserRestore)")
    public void configureRestoreFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof AuthenticatedRequestBean)) {
            throw new RuntimeException(
                    String.format("Could not configure RestoreFactory with invalid request %s", Arrays.toString(args)));
        }
        AuthenticatedRequestBean requestBean = (AuthenticatedRequestBean) args[0];

        HqAuth auth = getHqAuth((String) args[1]);

        configureRestoreFactory(requestBean, auth);
        configureSentryScope(restoreFactory);

        if (requestBean.isMustRestore()) {
            restoreFactory.performTimedSync();
        }
    }

    private void configureSentryScope(RestoreFactory restoreFactory) {
        Sentry.configureScope(scope -> {
            scope.setTag(Constants.AS_USER, restoreFactory.getEffectiveUsername());
            scope.setExtra(Constants.USER_SYNC_TOKEN, restoreFactory.getSyncToken());
            scope.setExtra(Constants.USER_SANDBOX_PATH, restoreFactory.getSQLiteDB().getDatabaseFileForDebugPurposes());
        });
    }

    // Package-private for testing.
    void configureRestoreFactory(AuthenticatedRequestBean requestBean, HqAuth auth) throws Exception {
        Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
        if (userDetails.isPresent() && userDetails.get().isPublicSession()) {
            // A public web apps session restores only as its own user; never use client-supplied values
            HqUserDetailsBean details = userDetails.get();
            restoreFactory.configure(details.getUsername(), details.getDomain(), null, auth);
            return;
        }
        if (requestBean.getRestoreAsCaseId() != null) {
            // SMS user filling out a form as a case
            restoreFactory.configure(requestBean.getDomain(), requestBean.getRestoreAsCaseId(), auth);
            return;
        }
        if (requestBean.getUsername() != null && requestBean.getDomain() != null) {
            // Normal restore path
            restoreFactory.configure(requestBean, auth);
            final Span span = GlobalTracer.get().activeSpan();
            if (span != null && (span instanceof MutableSpan)) {
                MutableSpan localRootSpan = ((MutableSpan) span).getLocalRootSpan();
                localRootSpan.setTag("domain", requestBean.getDomain());
                localRootSpan.setTag("user", requestBean.getUsername());
            }
        } else if (requestBean instanceof SessionRequestBean){
            // SMS users don't submit username and domain with each request, so obtain from session
            String sessionId = ((SessionRequestBean) requestBean).getSessionId();
            SerializableFormSession formSession = formSessionService.getSessionById(sessionId);

            if (formSession.getRestoreAsCaseId() != null) {
                restoreFactory.configure(formSession.getDomain(), formSession.getRestoreAsCaseId(), auth);
            } else {
                restoreFactory.configure(formSession.getUsername(), formSession.getDomain(), formSession.getAsUser(), auth);
            }
        } else {
            throw new Exception("Unable to configure restore factory");
        }
    }

    @After(value = "@annotation(org.commcare.formplayer.annotations.UserRestore)")
    public void closeRestoreFactory(JoinPoint joinPoint) throws Throwable {
        restoreFactory.getSQLiteDB().closeConnection();
        if (restoreFactory.shouldDeletePublicSandboxOnClose()) {
            try {
                restoreFactory.getSQLiteDB().deleteDatabaseFolder();
            } catch (Exception e) {
                log.error("Failed to delete public web apps session sandbox", e);
            }
        }
    }

    // Package-private for testing.
    HqAuth getHqAuth(String sessionToken) {
        // A public web apps session has no Django sessionid; authenticate its outbound HQ calls
        // with the public session key instead. Gate this on the HMAC-authenticated `public` field
        // from HQ's session_details response, never on the client-supplied header.
        Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
        if (userDetails.isPresent() && userDetails.get().isPublicSession()) {
            return new PublicFormSessionAuth(userDetails.get().getAuthToken());
        }
        if (sessionToken != null) {
            return new DjangoAuth(sessionToken);
        }
        // Null auth expected for SMS requests
        return null;
    }

}
