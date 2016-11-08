package aspects;

import beans.AuthenticatedRequestBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Created by willpride on 11/7/16.
 */
@Aspect
public class LockAspect {

    @Autowired
    protected LockRegistry userLockRegistry;

    Lock lock;

    @Before(value = "@annotation(annotations.UserLock)")
    public void beforeLock(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            lock = getLockAndBlock(TableBuilder.scrubName(bean.getUsername()));
        }
    }

    @After(value = "@annotation(annotations.UserLock)")
    public void afterLock(JoinPoint joinPoint) {
        lock.unlock();
    }

    protected Lock getLockAndBlock(String username){
        Lock lock = userLockRegistry.obtain(username);
        obtainLock(lock);
        return lock;
    }

    protected boolean obtainLock(Lock lock) {
        try {
            return lock.tryLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            return obtainLock(lock);
        }
    }
}
