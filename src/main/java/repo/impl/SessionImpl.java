package repo.impl;

import objects.SerializableSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import repo.SessionRepo;

import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
public class SessionImpl implements SessionRepo{

    @Autowired
    private RedisTemplate<String, SerializableSession> redisTemplate;

    private static String SESSION_KEY = "Session";

    @Override
    public void save(SerializableSession session) {
        System.out.println("Saving session: " + session.getId());
        this.redisTemplate.opsForHash().put(SESSION_KEY, session.getId(), session);
    }

    @Override
    public SerializableSession find(String id) {
        return (SerializableSession) this.redisTemplate.opsForHash().get(SESSION_KEY, id);
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
