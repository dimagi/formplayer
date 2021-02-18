package org.commcare.formplayer.services;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;
import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class FormplayerRedisLockRegistry implements LockRegistry {

    private final RedissonClient redisson;
    CountDownLatch latch = new CountDownLatch(1);

    class LockMetadata {
        public String lockId;
        public String serverName = "server1";
        public Integer serverThreadId = 14;

        public LockMetadata(String lockId) {
            this.lockId = lockId;
        }
    }

    public FormplayerRedisLockRegistry() {
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redisson = Redisson.create(config);
        RTopic topic = redisson.getTopic("topic");
        topic.addListener(LockMetadata.class, new MessageListener<LockMetadata>() {
            @Override
            public void onMessage(CharSequence channel, LockMetadata lockMetadata) {
                System.out.println("debug to be removed " + channel.toString());
                receiveEvictMessage(lockMetadata);
            }
        });
        System.out.println("debug to be removed");
    }

    @Override
    public Lock obtain(Object lockKey) {
        return obtain(lockKey, true);
    }

    public Lock obtain(Object lockKey, boolean writeLock) {
        ReadWriteLock rwLock = this.redisson.getReadWriteLock(lockKey.toString());
        if (writeLock) {
            return rwLock.writeLock();
        }
        return rwLock.readLock();
    }

    public boolean obtainAndLock(Object lockKey, boolean writeLock) {
        String lockKeyString = lockKey.toString();
        Lock lock = this.obtain(lockKeyString, writeLock);

        boolean didLock = false;
        for (int i = 0; i < 3; i++) {
            try {
                didLock = lock.tryLock(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            RBucket<LockMetadata> bucket = redisson.getBucket(lockKeyString);
            if (didLock) {
                // TODO: Check if there is something in bucket already
                bucket.set(new LockMetadata(lockKeyString), 100, TimeUnit.SECONDS);
                return didLock;
            } else {
                LockMetadata lockMetadata = bucket.get();
                if (lockMetadata != null) {
                    // TODO: set time check
                    if (true) {
                        throw new Error("Lock currently held within lease time");
                    } else {
                        sendEvictMessage(lockMetadata);
                    }
                }
            }
        }

        return didLock;
    }



    private void sendEvictMessage(LockMetadata lockMetadata) {
        RTopic topic = redisson.getTopic("topic");
        topic.publish(lockMetadata);
    }

    private void receiveEvictMessage(LockMetadata lockMetadata) {
        // TODO: Check if severname is this server
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
