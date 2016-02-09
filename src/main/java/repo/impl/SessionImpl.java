package repo.impl;

import objects.SerializableFormSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import repo.SessionRepo;

import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
public class SessionImpl implements SessionRepo{

    @Autowired
    private RedisTemplate<String, SerializableFormSession> redisTemplate;

    private static String SESSION_KEY = "Session";

    @Override
    public void save(SerializableFormSession session) {
        this.redisTemplate.opsForHash().put(SESSION_KEY, session.getId(), session);
    }

    @Override
    public SerializableFormSession find(String id) {
        return (SerializableFormSession) this.redisTemplate.opsForHash().get(SESSION_KEY, id);
    }

    @Override
    public Map<Object, Object> findAll() {
        return this.redisTemplate.opsForHash().entries(SESSION_KEY);

    }

    @Override
    public void delete(String id) {
        this.redisTemplate.opsForHash().delete(SESSION_KEY,id);
    }
}
