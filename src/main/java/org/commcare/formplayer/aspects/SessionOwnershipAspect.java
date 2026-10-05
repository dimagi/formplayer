package org.commcare.formplayer.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.util.Arrays;

/**
 * Enforces that the authenticated caller owns the form session they address by id.
 *
 * <p>Formplayer authenticates the caller's username and domain (CommCareSessionAuthFilter ->
 * HqUserDetailsService), but the by-id session lookups it relies on
 * ({@link FormSessionService#getSessionById} / {@link FormSessionService#deleteSessionById}) resolve
 * purely by primary key. Without this check any authenticated user can read and attempt to mutate
 * another user's form session, including from a different domain, given only its id.
 *
 * <p>The caller's identity is taken from the request bean's username/domain, which the
 * authentication filter has already bound to the authenticated session. This mirrors
 * how {@link UserRestoreAspect} and {@link LockAspect} already derive identity from the bean.
 *
 * <p>Ordered after {@link PublicSessionLockAspect} (0), which pins a public session's bean identity
 * to the authenticated principal, so this check compares against the pinned identity rather than the
 * raw client value. Still ahead of {@link LockAspect} (2) and {@link UserRestoreAspect} (5) so a
 * foreign session is refused before either derives a lock key or restore context from it.
 */
@Aspect
@Order(1)
public class SessionOwnershipAspect {

    private static final Log log = LogFactory.getLog(SessionOwnershipAspect.class);

    @Autowired
    private FormSessionService formSessionService;

    @Before(value = "@annotation(org.commcare.formplayer.annotations.ValidateSessionOwner)")
    public void validateSessionOwner(JoinPoint joinPoint) {
        // A handler binds at most one request body, so there is only ever one SessionRequestBean
        // argument; find it wherever it sits rather than assuming a fixed position.
        SessionRequestBean bean = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof SessionRequestBean)
                .map(arg -> (SessionRequestBean)arg)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Cannot validate session ownership: no SessionRequestBean argument on "
                                + joinPoint.getSignature()));
        String sessionId = bean.getSessionId();

        // HMAC-signed requests (SMS and HQ-proxied Edit Forms) derive their identity from
        // the session itself and are trusted via the shared HMAC key rather than by user
        // credentials, so ownership is not meaningful for them.
        if (RequestUtils.requestAuthedWithHmac()) {
            return;
        }

        // For cookie-authenticated request, the HqUserDetailsService validates that the
        // bean's username+domain exist and rejects the request before this point if
        // either is missing. Treat their absence as unauthorized.
        String callerUsername = bean.getUsername();
        String callerDomain = bean.getDomain();
        if (callerUsername == null || callerDomain == null) {
            throw new FormNotFoundException(sessionId);
        }

        SerializableFormSession session = formSessionService.getSessionById(sessionId);
        boolean owned = callerDomain.equals(session.getDomain())
                && TableBuilder.scrubName(callerUsername).equals(session.getUsername());
        if (!owned) {
            log.warn(String.format(
                    "Blocked user '%s' in domain '%s' from accessing form session %s owned by '%s' "
                            + "in domain '%s'",
                    TableBuilder.scrubName(callerUsername), callerDomain, sessionId,
                    session.getUsername(), session.getDomain()));
            // behaves the same as if the session does not exist
            throw new FormNotFoundException(sessionId);
        }
    }
}
