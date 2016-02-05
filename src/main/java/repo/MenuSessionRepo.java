package repo;

import objects.SerializableMenuSession;

/**
 * Created by willpride on 2/5/16.
 */
public interface MenuSessionRepo {
    public void save (SerializableMenuSession session);
    public SerializableMenuSession find(String id);
    public java.util.Map<Object, Object> findAll();
    public void delete(String id);
}
