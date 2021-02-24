package org.commcare.formplayer.services;

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
import java.util.concurrent.locks.ReadWriteLock;

public class FormplayerRedisLockRegistry implements LockRegistry {

    CountDownLatch latch = new CountDownLatch(1);
    private static final String REDIS_TOPIC_EVICT = "EVICT";

    private final Map<String, FormplayerRedisLock> locks = new ConcurrentHashMap<>();

    private final RedissonClient redisson;
    private final String serverName;

    private int tryLockDurationInSeconds;
    private int nonEvictionDurationInSeconds;

    public FormplayerRedisLockRegistry(String serverName, String redisIpcTopic, int tryLockDurationInSeconds,
                                       int nonEvictionDurationInSeconds) {
        this.serverName = serverName;
        this.tryLockDurationInSeconds = tryLockDurationInSeconds;
        this.nonEvictionDurationInSeconds = nonEvictionDurationInSeconds;

        // TODO: make configurable.
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redisson = Redisson.create(config);

        RTopic topic = redisson.getTopic(redisIpcTopic);

        FormplayerRedisLockRegistry registry = this;
        topic.addListener(RedisTopicMessage.class, new MessageListener<RedisTopicMessage>() {
            @Override
            public void onMessage(CharSequence channel, RedisTopicMessage redisTopicMessage) {
                int x = 3;
                try {
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
        // TODO: Check lockKey type string
        return this.locks.computeIfAbsent(lockKey.toString(), k -> {
            RReadWriteLock rwLock = this.redisson.getReadWriteLock((String) lockKey);
            LockMetadata lockMetadata = new LockMetadata(lockKey.toString(),
                    this.serverName);
            if (writeLock) {
                return new FormplayerRedisLock(rwLock, lockMetadata, this.redisson, this, REDIS_TOPIC_EVICT,
                        this.tryLockDurationInSeconds, this.nonEvictionDurationInSeconds);
            }
            return new FormplayerRedisLock(rwLock, lockMetadata, this.redisson, this, REDIS_TOPIC_EVICT,
                    this.tryLockDurationInSeconds, this.nonEvictionDurationInSeconds);
        });
    }

    public void removeLock(FormplayerRedisLock lock) {
        this.locks.remove(lock.getId());
    }

    public void sendEvictMessage(LockMetadata lockMetadata, String redisIpcTopic) {
        RTopic topic = this.redisson.getTopic(redisIpcTopic);
        topic.publish(new RedisTopicMessage(redisIpcTopic, lockMetadata));
    }

    public void receiveEvictMessage(RedisTopicMessage msg, String serverName) {
        if (msg.action != REDIS_TOPIC_EVICT) return;

        LockMetadata lockMetadata = msg.lockMetadata;
        if (lockMetadata != null && lockMetadata.serverName == serverName) {
            FormplayerRedisLock lock = this.locks.get(lockMetadata.lockId);
            Thread ownerThread = lock.getOwner();
            if (ownerThread.getId() == lockMetadata.serverThreadId) {
                if (this.redisson.getLock(lockMetadata.lockId).isHeldByThread(ownerThread.getId()) &&
                        ownerThread.isAlive()) {
                    ownerThread.interrupt();
                }
            }
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

        public final RReadWriteLock lock;
        private final LockMetadata lockMetadata;
        private final RedissonClient redisson;
        private final FormplayerRedisLockRegistry registry;
        private final String redisIpcTopic;
        private Thread ownerThread;

        private final int tryLockDurationInSeconds;
        private final int nonEvictionDurationInSeconds;
        private final int lockTryTimes = 3;

        public FormplayerRedisLock(RReadWriteLock lock,
                                   LockMetadata lockMetadata,
                                   RedissonClient redisson,
                                   FormplayerRedisLockRegistry registry,
                                   String redisIpcTopic,
                                   int tryLockDurationInSeconds,
                                   int nonEvictionDurationInSeconds) {
            this.lock = lock;
            this.lockMetadata = lockMetadata;
            this.redisson = redisson;
            this.registry = registry;
            this.redisIpcTopic = redisIpcTopic;
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
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            boolean didLock = false;
            for (int i = 0; i < this.lockTryTimes; i++) {
                try {
                    didLock = this.lock.writeLock().tryLock(time, unit);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                RBucket<LockMetadata> bucket = this.redisson.getBucket(lockMetadata.lockId);
                if (didLock) {
                    // TODO: Check if there is something in bucket already
                    this.lockMetadata.setLockedAt(System.currentTimeMillis());
                    this.lockMetadata.setServerThreadId(Thread.currentThread().getId());
                    this.ownerThread = Thread.currentThread();
                    // bucket.set(this.lockMetadata, this.nonEvictionDurationInSeconds, TimeUnit.SECONDS);
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
            // Use a freshly acquired RLock to check if it's held (`isHeldByCurrentThread` not a method
            // of redis read and write locks)
            RLock rLock = this.redisson.getLock(this.lockMetadata.lockId);
            if (rLock.isHeldByCurrentThread()) {
                RBucket<LockMetadata> bucket = this.redisson.getBucket(this.lockMetadata.lockId);
                LockMetadata retrievedlockMetadata = bucket.getAndDelete();
                this.registry.removeLock(this);
                // TODO: log
                rLock.unlock();
            }
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public RLock getRLock() {
            return (RLock) this.lock.writeLock();
        }

        public String getId() {
            return this.lockMetadata.lockId;
        }

        public long getThreadId() {
            return this.lockMetadata.serverThreadId;
        }

        public Thread getOwner() {
            return this.ownerThread;
        }
    }


}
