package org.commcare.formplayer.tests;

import org.commcare.formplayer.services.FormplayerRedisLockRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.commcare.formplayer.services.FormplayerRedisLockRegistry.REDIS_TOPIC_EVICT;

public class FormplayerRedisLockRegistryTest {

    private FormplayerRedisLockRegistry registry;
    private RedissonClient redisson;

    private static final String SERVER_NAME = "S";
    private static final String REDIS_KEY_PREFIX = "TEST_PREFIX_";
    private static final String REDIS_IPC_TOPIC = "topic_";

    @BeforeEach
    public void setUp() throws Exception {
        this.registry = new FormplayerRedisLockRegistry(SERVER_NAME, REDIS_KEY_PREFIX, REDIS_IPC_TOPIC, 1, 100);

        final Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redisson = Redisson.create(config);

        this.redisson.getKeys().deleteByPattern(REDIS_KEY_PREFIX + "*");
    }

    @Test
    public void testSingleThreadLockUnlock() throws InterruptedException {
        final CountDownLatch waitForThread = new CountDownLatch(1);
        final CountDownLatch waitForCheck = new CountDownLatch(1);

        final String lockName = "lock1";

        FormplayerRedisLockRegistry.FormplayerRedisLock formplayerRedisLock = registry.obtain(lockName);

        Thread t = new Thread() {
            public void run() {
                try {
                    assert(formplayerRedisLock.tryLock(1, TimeUnit.SECONDS));
                    waitForThread.countDown();
                    // Wait for main thread to run checks on the lock.
                    waitForCheck.await();
                    formplayerRedisLock.unlock();
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
        // Check that bucket has entries.
        RBucket<FormplayerRedisLockRegistry.LockMetadata> bucket = this.redisson.getBucket(this.registry.lockNameToRedisBucketKey(lockName));
        FormplayerRedisLockRegistry.LockMetadata lockMetadata = bucket.get();
        assert(lockMetadata.lockId.equals(lockName) &&
                lockMetadata.serverName.equals(SERVER_NAME) &&
                lockMetadata.serverThreadId == t.getId());

        waitForCheck.countDown();

        // Unlocking happens here.
        t.join();
        // Check that it is unlocked and bucket is empty.
        assert(!rLock.isLocked());
        lockMetadata = bucket.get();
        assert(lockMetadata == null);
    }

    @Test
    public void testTwoThreadsMutualExclusionUseDifferentRegistries() throws InterruptedException {
        testTwoThreadsMutualExclusion(true);
    }

    @Test
    public void testTwoThreadsMutualExclusionUseSameRegistries() throws InterruptedException {
        testTwoThreadsMutualExclusion(false);
    }

    private void testTwoThreadsMutualExclusion(boolean newRegistry) throws InterruptedException {
        final String lockName = "lock1";
        final long DURATION_BETWEEN_THREADS = 1000;

        List<Entry> entries = Collections.synchronizedList(new ArrayList<Entry>());

        final CountDownLatch waitForT1 = new CountDownLatch(1);

        Thread t1 = new Thread() {
            public void run() {
                FormplayerRedisLockRegistry threadRegistry = (newRegistry) ?
                        new FormplayerRedisLockRegistry("S1", REDIS_KEY_PREFIX, REDIS_IPC_TOPIC, 1, 100) :
                        registry;
                Lock lock = threadRegistry.obtain(lockName);
                try {
                    assert(lock.tryLock());

                    // Allow T2 to run after we've acquired the lock
                    waitForT1.countDown();
                    Thread.sleep(DURATION_BETWEEN_THREADS/3);
                    entries.add(new Entry("T1", System.currentTimeMillis()));
                    Thread.sleep(DURATION_BETWEEN_THREADS);

                } catch (InterruptedException e) {
                } finally {
                    lock.unlock();
                }

            };
        };

        Thread t2 = new Thread() {
            public void run() {
                FormplayerRedisLockRegistry threadRegistry = (newRegistry) ?
                        new FormplayerRedisLockRegistry("S2", REDIS_KEY_PREFIX, REDIS_IPC_TOPIC, 1, 100) :
                        registry;
                Lock lock = threadRegistry.obtain(lockName);
                try {
                    // Wait for T1 to acquire the lock
                    waitForT1.await();

                    assert(lock.tryLock((long) (DURATION_BETWEEN_THREADS * 1.5), TimeUnit.MILLISECONDS));

                    entries.add(new Entry("T2", System.currentTimeMillis()));
                } catch (InterruptedException e) {
                } finally {
                    lock.unlock();
                }
            };
        };

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        Entry e1 = entries.get(0);
        Entry e2 = entries.get(1);
        assert(e1.name == "T1");
        assert(e2.name == "T2");
        assert((e2.timestamp - e1.timestamp) >= DURATION_BETWEEN_THREADS);
    }

    @Test
    public void testEviction() throws InterruptedException {
        final String lockName = "lock1";
        final long NON_EVICTION_DURATION = 1000;

        // shouldBeTrue needs to be flipped during running
        final boolean[] shouldBeTrue = {false, false};
        // Listen to the eviction channel and make sure we see an eviction message.
        RTopic topic = redisson.getTopic(REDIS_KEY_PREFIX + REDIS_IPC_TOPIC);
        topic.addListener(FormplayerRedisLockRegistry.RedisTopicMessage.class, new MessageListener<FormplayerRedisLockRegistry.RedisTopicMessage>() {
            @Override
            public void onMessage(CharSequence channel, FormplayerRedisLockRegistry.RedisTopicMessage redisTopicMessage) {
                FormplayerRedisLockRegistry.LockMetadata lockMetadata = redisTopicMessage.lockMetadata;
                // eviction message received, flip the first bool
                if (redisTopicMessage.action.equals(REDIS_TOPIC_EVICT) && lockMetadata.lockId.equals(lockName) &&
                        lockMetadata.serverName.equals("S1")) {
                    shouldBeTrue[0] = true;
                }
            }
        });

        List<Entry> entries = Collections.synchronizedList(new ArrayList<Entry>());
        final CountDownLatch waitForT1 = new CountDownLatch(1);
        final CountDownLatch waitForT2 = new CountDownLatch(1);

        Thread t1 = new Thread() {
            public void run() {
                FormplayerRedisLockRegistry threadRegistry =
                        new FormplayerRedisLockRegistry("S1", REDIS_KEY_PREFIX, REDIS_IPC_TOPIC, 1, (int) (NON_EVICTION_DURATION/1000));
                Lock lock = threadRegistry.obtain(lockName);
                try {

                    assert(lock.tryLock());

                    waitForT1.countDown();
                    entries.add(new Entry("T1", System.currentTimeMillis()));

                    // We should be interrupted here and the rest should not run.
                    waitForT2.await();
                    entries.add(new Entry("SHOULD NOT BE ADDED", System.currentTimeMillis()));
                    lock.unlock();
                } catch (InterruptedException e) {
                    // needs to be interrupted
                    shouldBeTrue[1] = true;
                } finally {
                    lock.unlock();
                }

            };
        };

        Thread t2 = new Thread() {
            public void run() {
                FormplayerRedisLockRegistry threadRegistry =
                        new FormplayerRedisLockRegistry("S2", REDIS_KEY_PREFIX, REDIS_IPC_TOPIC, 1, 100);
                Lock lock = threadRegistry.obtain(lockName);
                try {
                    waitForT1.await();

                    // Sleep for the non eviction duration which ensures we will evict when we try to lock
                    Thread.sleep((long) (NON_EVICTION_DURATION));
                    assert(lock.tryLock());

                    entries.add(new Entry("T2", System.currentTimeMillis()));
                } catch (InterruptedException e) {
                } finally {
                    lock.unlock();
                }
            };
        };

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        Entry e1 = entries.get(0);
        Entry e2 = entries.get(1);
        assert(e1.name == "T1");
        assert(e2.name == "T2");
        assert(entries.size() == 2);
        assert(shouldBeTrue[0] && shouldBeTrue[1]);
    }

    class Entry {
        public final String name;
        public final long timestamp;

        public Entry(String name, long timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }

}
