package services;

import org.joda.time.DateTime;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;
import util.Constants;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by willpride on 11/6/17.
 */
public class FormplayerLockRegistry implements LockRegistry {

    private final FormplayerReentrantLock[] lockTable;

    private final int mask;

    public FormplayerLockRegistry() {
        this(0xFF);
    }

    public FormplayerLockRegistry(int mask) {
        String bits = Integer.toBinaryString(mask);
        Assert.isTrue(bits.length() < 32 && (mask == 0 || bits.lastIndexOf('0') < bits.indexOf('1')), "Mask must be a power of 2 - 1");
        this.mask = mask;
        int arraySize = this.mask + 1;
        this.lockTable = new FormplayerReentrantLock[arraySize];
        for (int i = 0; i < arraySize; i++) {
            this.lockTable[i] = new FormplayerReentrantLock();
        }
    }

    private FormplayerReentrantLock setNewLock(Integer lockIndex) {
        this.lockTable[lockIndex] = new FormplayerReentrantLock();
        return this.lockTable[lockIndex];
    }

    private boolean threadLives(Thread thread) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        return threadSet.contains(thread);
    }

    @Override
    public FormplayerReentrantLock obtain(Object lockKey) {
        Assert.notNull(lockKey, "'lockKey' must not be null");
        Integer lockIndex = lockKey.hashCode() & this.mask;
        FormplayerReentrantLock lock = this.lockTable[lockIndex];

        Thread ownerThread = lock.getOwner();

        if (!threadLives(ownerThread)) {
            lock = setNewLock(lockIndex);
        }
        if (lock.isExpired()) {
            lock = setNewLock(lockIndex);
        }
        return lock;
    }

    public class FormplayerReentrantLock extends ReentrantLock {

        DateTime lockTime;

        @Override
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            boolean obtainedLock = super.tryLock(timeout, unit);
            if (obtainedLock) {
                lockTime = new DateTime();
            }
            return obtainedLock;
        }

        public Thread getOwner() {
            return super.getOwner();
        }

        public boolean isExpired() {
            return new DateTime().minusMillis(Constants.LOCK_DURATION).isAfter(lockTime);
        }
    }
}
