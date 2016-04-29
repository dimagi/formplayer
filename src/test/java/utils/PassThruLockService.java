package utils;

import org.springframework.integration.support.locks.PassThruLockRegistry;
import org.springframework.stereotype.Component;
import services.LockService;

import java.util.concurrent.locks.Lock;

/**
 * Created by willpride on 3/11/16.
 */
@Component
public class PassThruLockService implements LockService {
    @Override
    public Lock getLock(Object key) {
        System.out.println("getting pass thru registry");
        return new PassThruLockRegistry().obtain(key);
    }
}
