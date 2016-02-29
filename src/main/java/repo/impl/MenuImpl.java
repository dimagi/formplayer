package repo.impl;

import objects.SerializableMenuSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import repo.MenuRepo;

import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
public class MenuImpl implements MenuRepo {

    @Autowired
    private RedisTemplate<String, SerializableMenuSession> redisTemplate;

    private static String MENU_SESSION_KEY = "Menu-session";

    @Override
    public void save(SerializableMenuSession session) {
        this.redisTemplate.opsForHash().put(MENU_SESSION_KEY, session.getSessionId(), session);
    }

    @Override
    public SerializableMenuSession find(String id) {
        return (SerializableMenuSession) this.redisTemplate.opsForHash().get(MENU_SESSION_KEY, id);
    }

    @Override
    public Map<Object, Object> findAll() {
        return this.redisTemplate.opsForHash().entries(MENU_SESSION_KEY);

    }

    @Override
    public void delete(String id) {
        this.redisTemplate.opsForHash().delete(MENU_SESSION_KEY,id);
    }
}
