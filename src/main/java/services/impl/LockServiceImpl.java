package services.impl;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;
import services.LockService;

import java.util.concurrent.locks.Lock;

/**
 * Created by willpride on 3/11/16.
 */
@Component
@EnableAutoConfiguration
public class LockServiceImpl implements LockService {

    private static RedisLockRegistry mLockRegistry;

    JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();

    @Override
    public Lock getLock(Object key) {
        if(mLockRegistry == null){
            mLockRegistry = new RedisLockRegistry(jedisConnectionFactory, "form-session");
        }
        Lock lock = mLockRegistry.obtain(key);
        jedisConnectionFactory.destroy();
        return lock;
    }
}
