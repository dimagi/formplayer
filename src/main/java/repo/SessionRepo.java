package repo;

import objects.SerializableFormSession;

/**
 * Created by willpride on 1/19/16.
 */
public interface SessionRepo {
    public void save (SerializableFormSession session);
    public SerializableFormSession find(String id);
    public java.util.Map<Object, Object> findAll();
    public void delete(String id);
}
