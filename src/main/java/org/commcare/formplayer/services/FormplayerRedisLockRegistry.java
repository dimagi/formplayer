package org.commcare.formplayer.services;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;
import org.springframework.integration.support.locks.LockRegistry;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class FormplayerRedisLockRegistry implements LockRegistry {

    CountDownLatch latch = new CountDownLatch(1);
    private static final String REDIS_TOPIC_EVICT = "EVICT";

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
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence channel, String redisTopicMessage) {
                int x = 3;
                try {
                    receiveEvictMessage(redisson, null, serverName);
                } catch(Exception e) {
                    // TODO: Handle
                }

            }
        });
    }

    @Override
    public Lock obtain(Object lockKey) {
        return obtain(lockKey, true);
    }

    public Lock obtain(Object lockKey, boolean writeLock) {
        // TODO: Check lockKey type string
        ReadWriteLock rwLock = this.redisson.getReadWriteLock((String) lockKey);
        LockMetadata lockMetadata = new LockMetadata(lockKey.toString(),
                                                     this.serverName,
                                                     Thread.currentThread().getId());
        if (writeLock) {
            return new FormplayerRedisLock(rwLock.writeLock(), lockMetadata, this.redisson, REDIS_TOPIC_EVICT,
                    this.tryLockDurationInSeconds, this.nonEvictionDurationInSeconds);
        }
        return new FormplayerRedisLock(rwLock.readLock(), lockMetadata, this.redisson, REDIS_TOPIC_EVICT,
                this.tryLockDurationInSeconds, this.nonEvictionDurationInSeconds);
    }

    private static void sendEvictMessage(RedissonClient redisson, LockMetadata lockMetadata, String redisIpcTopic) {
        RTopic topic = redisson.getTopic(redisIpcTopic);
        topic.publish(new RedisTopicMessage(redisIpcTopic, lockMetadata));
    }

    private static void receiveEvictMessage(RedissonClient redisson, RedisTopicMessage msg, String serverName) {
        if (msg.action != REDIS_TOPIC_EVICT) return;

        LockMetadata lockMetadata = msg.lockMetadata;
        if (lockMetadata != null && lockMetadata.serverName == serverName) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getId() == lockMetadata.serverThreadId) {
                    if (redisson.getLock(lockMetadata.lockId).isHeldByThread(t.getId())
                            && t.isAlive()) {
                        t.interrupt();
                    }
                }
            }
        }
    }

    public static class LockMetadata implements Serializable {
        public final String lockId;
        public final String serverName;
        public final long serverThreadId;
        private volatile long lockedAt;

        public LockMetadata(String lockId, String serverName, long serverThreadId) {
            this.lockId = lockId;
            this.serverName = serverName;
            this.serverThreadId = serverThreadId;
        }

        public void setLockedAt(long lockedAt) {
            this.lockedAt = lockedAt;
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

        public final Lock lock;
        private final LockMetadata lockMetadata;
        private final RedissonClient redisson;
        private final String redisIpcTopic;

        private final int tryLockDurationInSeconds;
        private final int nonEvictionDurationInSeconds;
        private final int lockTryTimes = 3;

        public FormplayerRedisLock(Lock lock,
                                   LockMetadata lockMetadata,
                                   RedissonClient redisson,
                                   String redisIpcTopic,
                                   int tryLockDurationInSeconds,
                                   int nonEvictionDurationInSeconds) {
            this.lock = lock;
            this.lockMetadata = lockMetadata;
            this.redisson = redisson;
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
                    didLock = this.lock.tryLock(time, unit);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                RBucket<LockMetadata> bucket = this.redisson.getBucket(lockMetadata.lockId);
                if (didLock) {
                    // TODO: Check if there is something in bucket already
                    this.lockMetadata.setLockedAt(System.currentTimeMillis());
                    bucket.set(this.lockMetadata, this.nonEvictionDurationInSeconds, TimeUnit.SECONDS);
                    return didLock;
                } else {
                    LockMetadata retrievedlockMetadata = bucket.get();
                    if (retrievedlockMetadata != null) {
                        long lockedAt = retrievedlockMetadata.lockedAt;
                        long currentTime = System.currentTimeMillis();
                        if ((currentTime - lockedAt) <= this.nonEvictionDurationInSeconds) {
                            throw new Error("Lock currently held within lease time");
                        } else {
                            sendEvictMessage(this.redisson, retrievedlockMetadata, this.redisIpcTopic);
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
                // TODO: log
                rLock.unlock();
            }
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }


}
