package repo;

import objects.SerializableFormSession;
import org.springframework.data.repository.CrudRepository;

import java.io.IOException;
import java.util.List;

/**
 * Repository for storing and accessing form entry sessions
 */
public interface FormSessionRepo extends CrudRepository<SerializableFormSession, String> {
    List<SerializableFormSession> findUserSessions(String username);
}
