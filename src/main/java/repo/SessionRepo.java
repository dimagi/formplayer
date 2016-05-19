package repo;

import objects.SerializableFormSession;

/**
 * Created by willpride on 1/19/16.
 */
public interface SessionRepo {
    void save(SerializableFormSession session);
    SerializableFormSession find(String id);
    java.util.Map<Object, Object> findAll();
    void delete(String id);
}
