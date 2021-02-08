package org.commcare.formplayer.aspects;

import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.auth.HqAuth;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.RestoreFactory;

import java.util.Arrays;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
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
    protected FormSessionRepo formSessionRepo;

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

        if (requestBean.isMustRestore()) {
            restoreFactory.performTimedSync();
        }
    }

    private void configureRestoreFactory(AuthenticatedRequestBean requestBean, HqAuth auth) {
        if (requestBean.getRestoreAsCaseId() != null) {
            // SMS user filling out a form as a case
            restoreFactory.configure(requestBean.getDomain(), requestBean.getRestoreAsCaseId(), auth);
            return;
        }
        if (requestBean.getUsername() != null && requestBean.getDomain() != null) {
            // Normal restore path
            restoreFactory.configure(requestBean, auth, requestBean.getUseLiveQuery());
            final Span span = GlobalTracer.get().activeSpan();
            if (span != null && (span instanceof MutableSpan)) {
                MutableSpan localRootSpan = ((MutableSpan) span).getLocalRootSpan();
                localRootSpan.setTag("domain", requestBean.getDomain());
                localRootSpan.setTag("user", requestBean.getUsername());
            }
        } else {
            // SMS users don't submit username and domain with each request, so obtain from session
            SerializableFormSession formSession = formSessionRepo.findOneWrapped(requestBean.getSessionId());

            if (formSession.getRestoreAsCaseId() != null) {
                restoreFactory.configure(formSession.getDomain(), formSession.getRestoreAsCaseId(), auth);
            } else {
                restoreFactory.configure(formSession.getUsername(), formSession.getDomain(), formSession.getAsUser(), auth);
            }
        }
    }

    @After(value = "@annotation(org.commcare.formplayer.annotations.UserRestore)")
    public void closeRestoreFactory(JoinPoint joinPoint) throws Throwable {
        restoreFactory.getSQLiteDB().closeConnection();
    }

    private HqAuth getHqAuth(String sessionToken) {
        if (sessionToken != null) {
            return new DjangoAuth(sessionToken);
        }
        // Null auth expected for SMS requests
        return null;
    }

}
