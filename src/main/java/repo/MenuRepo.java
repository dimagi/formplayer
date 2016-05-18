package repo;

import objects.SerializableMenuSession;

/**
 * Created by willpride on 2/5/16.
 */
public interface MenuRepo {
    void save(SerializableMenuSession session);
    SerializableMenuSession find(String id);
    java.util.Map<Object, Object> findAll();
    void delete(String id);
}
