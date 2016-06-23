package repo;

import objects.SerializableFormSession;

import java.util.List;

/**
 * Created by willpride on 1/19/16.
 */
public interface SessionRepo {
    void save(SerializableFormSession session);
    SerializableFormSession find(String id);
    List<SerializableFormSession> findUserSessions(String username);
    java.util.Map<Object, Object> findAll();
    void delete(String id);
}
