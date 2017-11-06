package aspects;

import beans.AuthenticatedRequestBean;
import com.timgroup.statsd.StatsDClient;
import io.sentry.event.Event;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import services.CategoryTimingHelper;
import util.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
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
    private RedisLockRegistry userLockRegistry;

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerSentry raven;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RedisTemplate<String, RedisLock> redisLockTemplate;

    private final Log log = LogFactory.getLog(LockAspect.class);

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
                    log.info("Relinquished lock for key " + username);
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
        Lock lock = userLockRegistry.obtain(username);
        if (obtainLock(lock)) {
            log.info("Obtained lock for key " + username);
            return lock;
        } else {
            String holdingThreadName = getHoldingThread(username);
            if (!threadLives(holdingThreadName)) {
                log.error("Evicting thread " + holdingThreadName + " from lock with key " + username);
                evictLock(username);
                return getLockAndBlock(username);
            }
            throw new LockError();
        }
    }

    private boolean threadLives(String name) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (Thread thread: threadArray) {
            if (thread.getName().contains(name)) {
                return true;
            }
        }
        return false;
    }

    private String getHoldingThread(String username) {
        RedisLock redisLock = redisLockTemplate.boundValueOps(String.format("formplayer-user:%s", username)).get();
        if (redisLock != null) {
            return redisLock.threadName;
        }
        return null;
    }

    private void evictLock(String username) {
        redisLockTemplate.delete(String.format("formplayer-user:%s", username));
    }

    private boolean obtainLock(Lock lock) {
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
