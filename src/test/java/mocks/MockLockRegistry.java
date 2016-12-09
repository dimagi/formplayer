package mocks;

import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Mock lock that will always return true
 */
public class MockLockRegistry implements LockRegistry {
    @Override
    public Lock obtain(Object lockKey) {
        return new Lock() {
            @Override
            public void lock() {

            }

            @Override
            public void lockInterruptibly() throws InterruptedException {

            }

            @Override
            public boolean tryLock() {
                return true;
            }

            @Override
            public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
                return true;
            }

            @Override
            public void unlock() {

            }

            @Override
            public Condition newCondition() {
                return null;
            }
        };
    }
}
