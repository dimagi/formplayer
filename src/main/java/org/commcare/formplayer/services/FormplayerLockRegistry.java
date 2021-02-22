package org.commcare.formplayer.services;

import io.sentry.SentryLevel;
import org.commcare.formplayer.exceptions.InterruptedRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by willpride on 11/6/17.
 */
public class FormplayerLockRegistry implements LockRegistry {

    @Autowired
    private FormplayerSentry raven;

    private final FormplayerReentrantLock[] lockTable;
    // Lock objects for accessing the FormplayerReentrantLock table above
    // These objects do not change
    private final Object[] lockTableLocks;

    private final int mask;

    private final Log log = LogFactory.getLog(FormplayerLockRegistry.class);

    public FormplayerLockRegistry() {
        this(0xFFFF);
    }

    public FormplayerLockRegistry(int mask) {
        String bits = Integer.toBinaryString(mask);
        Assert.isTrue(bits.length() < 32 && (mask == 0 || bits.lastIndexOf('0') < bits.indexOf('1')), "Mask must be a power of 2 - 1");
        this.mask = mask;
        int arraySize = this.mask + 1;
        this.lockTable = new FormplayerReentrantLock[arraySize];
        this.lockTableLocks = new Object[arraySize];
        for (int i = 0; i < arraySize; i++) {
            this.lockTable[i] = new FormplayerReentrantLock();
            this.lockTableLocks[i] = new Object();
        }
    }

    private FormplayerReentrantLock setNewLock(Integer lockIndex) {
        synchronized (this.lockTableLocks[lockIndex]) {
            this.lockTable[lockIndex] = new FormplayerReentrantLock();
            return this.lockTable[lockIndex];
        }
    }

    private Integer getLockIndex(Object lockKey) {
        return lockKey.hashCode() & this.mask;
    }

    @Override
    public FormplayerReentrantLock obtain(Object lockKey) {
        Assert.notNull(lockKey, "'lockKey' must not be null");
        Integer lockIndex = getLockIndex(lockKey);

        synchronized (this.lockTableLocks[lockIndex]) {
            FormplayerReentrantLock lock = this.lockTable[lockIndex];
            Thread ownerThread = lock.getOwner();

            if (ownerThread == null) {
                return lock;
            }
            if (!ownerThread.isAlive()) {
                log.error(String.format("Evicted dead thread %s owning lockkey %s.", ownerThread, lockKey));
                lock = setNewLock(lockIndex);
                return lock;
            }
            if (lock.isExpired()) {
                evict(lock, lockKey);
            }
            return lock;
        }
    }

    private void evict(FormplayerReentrantLock lock, Object lockKey) {
        Thread ownerThread = lock.getOwner();
        log.error(String.format("Thread %s owns expired lock with lock key %s.", ownerThread, lockKey));
        ownerThread.interrupt();
        try {
            ownerThread.join(5000);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
        if (ownerThread.isAlive()) {
            log.error(String.format("Unable to evict thread %s owning expired lock with lock key %s.", ownerThread, lockKey));
            Exception e = new Exception("Unable to get expired lock, owner thread has stack trace");
            e.setStackTrace(ownerThread.getStackTrace());
            raven.sendRavenException(new Exception(e), SentryLevel.WARNING);
        }
    }

    /**
     * Forcibly break any existing locks for the current user. Returns the broken lock if one
     * existed
     */
    public boolean breakAnyExistingLocks(String key) {
        Integer lockIndex = getLockIndex(key);
        synchronized (this.lockTableLocks[lockIndex]) {

            FormplayerReentrantLock existingLock = obtain(key);
            if (!existingLock.isLocked()) {
                return false;
            } else {
                evict(existingLock, key);
                return true;
            }
        }
    }

    /**
     * return the number of seconds since the user's current lock was acquired
     */
    public Integer getTimeLocked(String key) {
        Integer lockIndex = getLockIndex(key);
        synchronized (this.lockTableLocks[lockIndex]) {

            FormplayerReentrantLock existingLock = obtain(key);
            if (!existingLock.isLocked()) {
                return null;
            } else {
                return existingLock.timeLocked();
            }
        }
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

        public int timeLocked() {
            return Seconds.secondsBetween(lockTime,new DateTime()).getSeconds();
        }
    }
}
