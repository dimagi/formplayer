package repo;

import objects.SerializableFormSession;

import java.io.IOException;
import java.util.List;

/**
 * Repository for storing and accessing form entry sessions
 */
public interface SessionRepo {
    void save(SerializableFormSession session) throws IOException;
    SerializableFormSession find(String id);
    List<SerializableFormSession> findUserSessions(String username);
    void delete(String id);
}
