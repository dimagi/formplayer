package aspects;

import beans.AuthenticatedRequestBean;
import beans.SessionRequestBean;
import com.timgroup.statsd.StatsDClient;
import io.sentry.event.Event;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import repo.FormSessionRepo;
import services.CategoryTimingHelper;
import services.FormplayerLockRegistry;
import services.FormplayerLockRegistry.FormplayerReentrantLock;
import util.Constants;
import util.FormplayerSentry;
import util.RequestUtils;
import util.UserUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Aspect for weaving locking for classes that require it
 */
@Aspect
@Order(2)
public class LockAspect {

    @Autowired
    private FormplayerLockRegistry userLockRegistry;

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerSentry raven;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private FormSessionRepo formSessionRepo;

    // needs to be accessible from WebAppContext.exceptionResolver
    public class LockError extends Exception {}

    private final Log log = LogFactory.getLog(LockAspect.class);

    @Around(value = "@annotation(annotations.UserLock)")
    public Object aroundLock(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (!(args[0] instanceof AuthenticatedRequestBean)) {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new RuntimeException(throwable);
            }
        }

        String username;
        AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];

        if (bean.getUsernameDetail() != null) {
            username = TableBuilder.scrubName(bean.getUsernameDetail());
        }
        else {
            SerializableFormSession formSession = formSessionRepo.findOneWrapped(bean.getSessionId());
            String tempUser = formSession.getUsername();
            String restoreAs = formSession.getAsUser();
            String restoreAsCaseId = formSession.getRestoreAsCaseId();
            if (restoreAsCaseId != null) {
                username = UserUtils.getRestoreAsCaseIdUsername(restoreAsCaseId);
            } else if (restoreAs != null) {
                username = tempUser + "_" + restoreAs;
            } else {
                username = tempUser;
            }
        }

        Lock lock;

        try {
            lock = getLockAndBlock(username);
            log.info(String.format("Obtained lock for username %s", username));
        } catch (LockError e) {
            logLockError(bean, joinPoint, "_timed_out");
            throw e;
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (lock != null) {
                try {
                    lock.unlock();
                    log.info(String.format("Relinquished lock for username %s", username));
                } catch (IllegalStateException e) {
                    // Lock was released after expiration
                    logLockError(bean, joinPoint, "_expired");
                    raven.sendRavenException(e, Event.Level.WARNING);
                }
            }
        }
    }

    private void logLockError(AuthenticatedRequestBean bean, ProceedingJoinPoint joinPoint, String lockIssue) {
        datadogStatsDClient.increment(
                Constants.DATADOG_ERRORS_LOCK,
                "domain:" + bean.getDomain(),
                "user:" + bean.getUsernameDetail(),
                "request:" + RequestUtils.getRequestEndpoint(request),
                "lock_issue:" + lockIssue
        );
    }

    private Lock getLockAndBlock(String username) throws LockError {
        FormplayerReentrantLock lock = userLockRegistry.obtain(username);
        if (obtainLock(lock)) {
            return lock;
        } else {
            log.info(String.format("Unable to obtain lock for username %s", username));
            throw new LockError();
        }
    }

    private boolean obtainLock(FormplayerReentrantLock lock) {
        CategoryTimingHelper.RecordingTimer timer = categoryTimingHelper.newTimer(Constants.TimingCategories.WAIT_ON_LOCK);
        timer.start();
        try {
            return lock.tryLock(Constants.USER_LOCK_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            return obtainLock(lock);
        } finally {
            timer.end()
                    .setMessage(timer.durationInMs() > 5 ? "Had to wait to obtain the lock"
                            : "Didn't have to wait for the lock")
                    .record();
        }
    }
}
