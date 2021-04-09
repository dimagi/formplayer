package org.commcare.formplayer.services;

import org.commcare.formplayer.exceptions.InterruptedRuntimeException;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;
import org.springframework.integration.support.locks.LockRegistry;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class FormplayerRedisLockRegistry implements LockRegistry {

    CountDownLatch latch = new CountDownLatch(1);
    public static final String REDIS_TOPIC_EVICT = "EVICT";

    private final Map<String, FormplayerRedisLock> locks = new ConcurrentHashMap<>();

    private final RedissonClient redisson;
    private final String serverName;
    private final String redisPrefix;
    private final String redisIpcTopic;

    private final int tryLockDurationInSeconds;
    private final int nonEvictionDurationInSeconds;

    public FormplayerRedisLockRegistry(String serverName, String redisPrefix, String redisIpcTopic, int tryLockDurationInSeconds,
                                       int nonEvictionDurationInSeconds) {
        this.serverName = serverName;
        this.redisPrefix = redisPrefix;
        this.redisIpcTopic = redisPrefix + redisIpcTopic;
        this.tryLockDurationInSeconds = tryLockDurationInSeconds;
        this.nonEvictionDurationInSeconds = nonEvictionDurationInSeconds;

        // TODO: make configurable.
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redisson = Redisson.create(config);

        RTopic topic = redisson.getTopic(this.redisIpcTopic);

        FormplayerRedisLockRegistry registry = this;
        topic.addListener(RedisTopicMessage.class, new MessageListener<RedisTopicMessage>() {
            @Override
            public void onMessage(CharSequence channel, RedisTopicMessage redisTopicMessage) {
                try {
                    if (redisTopicMessage.action.equals(REDIS_TOPIC_EVICT))
                        registry.receiveEvictMessage(redisTopicMessage, serverName);
                } catch(Exception e) {
                    // TODO: Handle
                }

            }
        });
    }

    @Override
    public FormplayerRedisLock obtain(Object lockKey) {
        return obtain(lockKey, true);
    }

    public FormplayerRedisLock obtain(Object lockKey, boolean writeLock) {
        return this.locks.computeIfAbsent(lockKey.toString(), k -> {
            RReadWriteLock rwLock = this.redisson.getReadWriteLock(this.lockNameToRedisLockKey(lockKey.toString()));
            LockMetadata lockMetadata = new LockMetadata(lockKey.toString(), this.serverName);
            if (writeLock) {
                return new FormplayerRedisLock(rwLock.writeLock(), lockMetadata, this.redisson, this, this.redisIpcTopic,
                        this.lockNameToRedisBucketKey(lockKey.toString()), this.tryLockDurationInSeconds, this.nonEvictionDurationInSeconds);
            }
            return new FormplayerRedisLock(rwLock.readLock(), lockMetadata, this.redisson, this, this.redisIpcTopic,
                    this.lockNameToRedisBucketKey(lockKey.toString()), this.tryLockDurationInSeconds, this.nonEvictionDurationInSeconds);
        });
    }

    public void removeLock(FormplayerRedisLock lock) {
        this.locks.remove(lock.getId());
    }

    public String lockNameToRedisLockKey(String lockName) {
        return this.redisPrefix + "LOCK_" + lockName;
    }

    public String lockNameToRedisBucketKey(String lockName) {
        return this.redisPrefix + "BUCKET_" + lockName;
    }

    public void sendEvictMessage(LockMetadata lockMetadata, String redisIpcTopic) {
        RTopic topic = this.redisson.getTopic(redisIpcTopic);
        topic.publish(new RedisTopicMessage(REDIS_TOPIC_EVICT, lockMetadata));
    }

    public void receiveEvictMessage(RedisTopicMessage msg, String serverName) {
        if (!msg.action.equals(REDIS_TOPIC_EVICT)) return;

        // TODO: Better logging

        // Servername check from reported lock metadata
        LockMetadata lockMetadata = msg.lockMetadata;
        if (lockMetadata == null || !lockMetadata.serverName.equals(serverName)) {
            return;
        }

        FormplayerRedisLock lock = this.locks.get(lockMetadata.lockId);

        if (lock == null) {
            // This is a special case if we removed the lock from locks before we unlocking
            return;
        }
        if ((System.currentTimeMillis() - lock.getLockedAt()) <= this.nonEvictionDurationInSeconds) {
            return;
        }

        Thread ownerThread = lock.getOwner();
        if (ownerThread.getId() != lockMetadata.serverThreadId) {
            // TODO: Re-evaulate if this is needed.
            return;
        }

        if (!ownerThread.isAlive() ||
                !this.locks.get(lockMetadata.lockId).getRLock().isHeldByThread(ownerThread.getId())) {
            return;
        }

        ownerThread.interrupt();
    }

    public Integer getTimeLocked(String key) {
        String redisBucketName = this.lockNameToRedisBucketKey(key);
        RBucket<LockMetadata> bucket = this.redisson.getBucket(redisBucketName);
        LockMetadata retrievedlockMetadata = bucket.get();
        if (retrievedlockMetadata != null) {
            return ((int) (System.currentTimeMillis() - retrievedlockMetadata.lockedAt));
        }
        return -1;
    }

    public boolean breakAnyExistingLocks(String key) {
        RLock lock = this.redisson.getLock(this.lockNameToRedisLockKey(key));
        if (!lock.isLocked()) {
            return false;
        } else {
            RBucket<LockMetadata> bucket = this.redisson.getBucket(this.lockNameToRedisBucketKey(key));
            LockMetadata retrievedlockMetadata = bucket.get();
            if (retrievedlockMetadata != null) {
                this.sendEvictMessage(retrievedlockMetadata, this.redisIpcTopic);
            }
            return true;
        }
    }

    public static class LockMetadata implements Serializable {
        public final String lockId;
        public final String serverName;
        public volatile long serverThreadId;
        private volatile long lockedAt;

        public LockMetadata(String lockId, String serverName) {
            this.lockId = lockId;
            this.serverName = serverName;
        }

        public void setLockedAt(long lockedAt) {
            this.lockedAt = lockedAt;
        }
        public void setServerThreadId(long serverThreadId) {
            this.serverThreadId = serverThreadId;
        }

    }

    public static class RedisTopicMessage implements Serializable {
        public final String action;
        public final LockMetadata lockMetadata;

        public RedisTopicMessage(String action, LockMetadata lockMetadata) {
            this.action = action;
            this.lockMetadata = lockMetadata;
        }
    }

    public static class FormplayerRedisLock implements Lock {

        public final RLock lock;
        private final LockMetadata lockMetadata;
        private final RedissonClient redisson;
        private final FormplayerRedisLockRegistry registry;
        private final String redisIpcTopic;
        private final String redisBucketName;
        private Thread ownerThread;

        private final int tryLockDurationInSeconds;
        private final int nonEvictionDurationInSeconds;
        private final int lockTryTimes = 3;

        public FormplayerRedisLock(RLock lock,
                                   LockMetadata lockMetadata,
                                   RedissonClient redisson,
                                   FormplayerRedisLockRegistry registry,
                                   String redisIpcTopic,
                                   String redisBucketName,
                                   int tryLockDurationInSeconds,
                                   int nonEvictionDurationInSeconds) {
            this.lock = lock;
            this.lockMetadata = lockMetadata;
            this.redisson = redisson;
            this.registry = registry;
            this.redisIpcTopic = redisIpcTopic;
            this.redisBucketName = redisBucketName;
            this.tryLockDurationInSeconds = tryLockDurationInSeconds;
            this.nonEvictionDurationInSeconds = nonEvictionDurationInSeconds;
        }

        @Override
        public void lock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock() {
            try {
                return tryLock(tryLockDurationInSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            }
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            boolean didLock = false;
            int lockTryTimes = 1;
            for (int i = 0; i < /*this.*/lockTryTimes; i++) {
                try {
                    // TODO: This is in a for loop so consider adjusting lock duration.
                    didLock = this.lock.tryLock(time, unit);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                RBucket<LockMetadata> bucket = this.redisson.getBucket(this.redisBucketName);
                if (didLock) {
                    // TODO: Check if there is something in bucket already
                    this.lockMetadata.setLockedAt(System.currentTimeMillis());
                    this.lockMetadata.setServerThreadId(Thread.currentThread().getId());
                    this.ownerThread = Thread.currentThread();
                    bucket.set(this.lockMetadata);
                    return didLock;
                } else {
                    LockMetadata retrievedlockMetadata = bucket.get();
                    if (retrievedlockMetadata != null) {
                        long lockedAt = retrievedlockMetadata.lockedAt;
                        long currentTime = System.currentTimeMillis();
                        if ((currentTime - lockedAt) <= this.nonEvictionDurationInSeconds) {
                            throw new Error("Lock currently held within lease time");
                        } else {
                            this.registry.sendEvictMessage(retrievedlockMetadata, this.redisIpcTopic);
                        }
                    }
                }
            }
            return didLock;
        }

        @Override
        public void unlock() {
            if (this.lock.isHeldByCurrentThread()) {
                RBucket<LockMetadata> bucket = this.redisson.getBucket(this.redisBucketName);
                LockMetadata retrievedlockMetadata = bucket.getAndDelete();
                this.lockMetadata.setServerThreadId(-1);
                this.lockMetadata.setLockedAt(-1);
                // TODO: log
                this.lock.unlock();
            }
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public RLock getRLock() {
            return (RLock) this.lock;
        }

        public String getId() {
            return this.lockMetadata.lockId;
        }

        public long getThreadId() {
            return this.lockMetadata.serverThreadId;
        }

        public long getLockedAt() {
            return this.lockMetadata.lockedAt;
        }

        public Thread getOwner() {
            return this.ownerThread;
        }

    }


}
