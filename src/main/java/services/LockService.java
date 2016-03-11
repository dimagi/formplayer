package services;

import java.util.concurrent.locks.Lock;

/**
 * Created by willpride on 3/11/16.
 */
public interface LockService {
    Lock getLock(Object key);
}
