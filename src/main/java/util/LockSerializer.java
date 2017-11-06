package util;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.ByteBuffer;

/**
 * Created by willpride on 10/31/17.
 */
public class LockSerializer implements RedisSerializer<RedisLock> {

    @Override
    public byte[] serialize(RedisLock t) throws SerializationException {
        int hostLength = t.lockHost.length;
        int keyLength = t.lockKey.length();
        int threadNameLength = t.threadName.length();
        byte[] value = new byte[1 + hostLength +
                1 + keyLength +
                1 + threadNameLength + 8];
        ByteBuffer buff = ByteBuffer.wrap(value);
        buff.put((byte) hostLength)
                .put(t.lockHost)
                .put((byte) keyLength)
                .put(t.lockKey.getBytes())
                .put((byte) threadNameLength)
                .put(t.threadName.getBytes())
                .putLong(t.lockedAt);
        return value;
    }

    @Override
    public RedisLock deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        byte[] host = new byte[buff.get()];
        buff.get(host);
        byte[] lockKey = new byte[buff.get()];
        buff.get(lockKey);
        byte[] threadName = new byte[buff.get()];
        buff.get(threadName);
        long lockedAt = buff.getLong();
        RedisLock lock = new RedisLock(new String(lockKey));
        lock.lockedAt = lockedAt;
        lock.lockHost = host;
        lock.threadName = new String(threadName);
        return lock;
    }

}