package aspects;

import beans.AuthenticatedRequestBean;
import com.timgroup.statsd.StatsDClient;
import io.sentry.event.Event;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.integration.support.locks.LockRegistry;
import services.CategoryTimingHelper;
import services.FormplayerLockRegistry;
import services.FormplayerLockRegistry.FormplayerReentrantLock;
import util.Constants;
import util.FormplayerSentry;
import util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
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

    // needs to be accessible from WebAppContext.exceptionResolver
    public class LockError extends Exception {}

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

        AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
        String username = TableBuilder.scrubName(bean.getUsernameDetail());
        Lock lock;

        try {
            lock = getLockAndBlock(username);
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
