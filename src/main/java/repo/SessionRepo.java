package repo;

import objects.SerializableSession;

/**
 * Created by willpride on 1/19/16.
 */
public interface SessionRepo {
    public void save (SerializableSession session);
    public SerializableSession find(String id);
    public java.util.Map<Object, Object> findAll();
    public void delete(String id);
}
