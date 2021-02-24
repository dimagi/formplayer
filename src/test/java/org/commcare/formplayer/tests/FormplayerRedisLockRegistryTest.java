package org.commcare.formplayer.tests;

import org.commcare.formplayer.services.FormplayerRedisLockRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class FormplayerRedisLockRegistryTest {

    private FormplayerRedisLockRegistry registry;
    private RedissonClient redisson;

    private static final String SERVER_NAME = "s";
    private static final String REDIS_KEY_PREFIX = "TEST_PREFIX_";
    private static final String REDIS_IPC_TOPIC = REDIS_KEY_PREFIX + "topic";

    @BeforeEach
    public void setUp() throws Exception {
        this.registry = new FormplayerRedisLockRegistry(SERVER_NAME, REDIS_IPC_TOPIC, 1, 100);

        final Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redisson = Redisson.create(config);

        this.redisson.getKeys().deleteByPattern(REDIS_KEY_PREFIX + "*");
    }

    public void testSingleThreadLockUnlock1() throws InterruptedException {
        final CountDownLatch waitForThread = new CountDownLatch(1);
        final CountDownLatch waitForCheck = new CountDownLatch(1);

        final String lockName = REDIS_KEY_PREFIX + "lock1";

        Thread t = new Thread() {
            public void run() {
                Lock lock = registry.obtain(lockName);

                try {
                    assert(lock.tryLock(1, TimeUnit.SECONDS));
                    waitForThread.countDown();

                    waitForCheck.await();
                    lock.unlock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            };
        };

        t.start();
        redisson.getTopic(REDIS_IPC_TOPIC).publish("hello");

        waitForThread.await();


//        // Check that lock is locked.
//        RLock lock = this.redisson.getLock(lockName);
//        assert(lock.isLocked());
//        assert(lock.isHeldByThread((t.getId())));
//        RBucket<FormplayerRedisLockRegistry.LockMetadata> bucket = this.redisson.getBucket(lockName);
//        FormplayerRedisLockRegistry.LockMetadata lockMetadata = bucket.get();
//        assert(lockMetadata.lockId == lockName &&
//                lockMetadata.serverName == SERVER_NAME &&
//                lockMetadata.serverThreadId == t.getId());
//
//        waitForCheck.countDown();
//
//        t.join();
//
//        // Check that lock is unlocked.
//        lock = this.redisson.getLock(lockName);
//        assert(!lock.isLocked());
//        lockMetadata = bucket.get();
//        assert(lockMetadata == null);
    }

    @Test
    public void testSingleThreadLockUnlock() throws InterruptedException {
        final CountDownLatch waitForThread = new CountDownLatch(1);
        final CountDownLatch waitForCheck = new CountDownLatch(1);

        final String lockName = REDIS_KEY_PREFIX + "lock1";

        FormplayerRedisLockRegistry.FormplayerRedisLock formplayerRedisLock = registry.obtain(lockName);

        RLock finalLock = formplayerRedisLock.getRLock();
        Thread t = new Thread() {
            public void run() {
                try {
                    assert(finalLock.tryLock(1, TimeUnit.SECONDS));
                    waitForThread.countDown();

                    waitForCheck.await();
                    finalLock.unlock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            };
        };

        t.start();

        waitForThread.await();

        RLock rLock = formplayerRedisLock.getRLock();
        // Check that lock is locked.
        assert(formplayerRedisLock.getRLock().isLocked());
        assert(rLock.isHeldByThread((t.getId())));

        RBucket<FormplayerRedisLockRegistry.LockMetadata> bucket = this.redisson.getBucket(lockName);
        FormplayerRedisLockRegistry.LockMetadata lockMetadata = bucket.get();
        assert(lockMetadata.lockId == lockName &&
                lockMetadata.serverName == SERVER_NAME &&
                lockMetadata.serverThreadId == t.getId());

        waitForCheck.countDown();

        t.join();

        assert(!rLock.isLocked());
        lockMetadata = bucket.get();
        assert(lockMetadata == null);

    }

}
