package repo;

import exceptions.FormNotFoundException;
import objects.SerializableFormSession;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository for storing and accessing form entry sessions
 */
public interface FormSessionRepo extends CrudRepository<SerializableFormSession, String> {
    List<SerializableFormSession> findUserSessions(String username);
    SerializableFormSession findOneWrapped(String id) throws FormNotFoundException;
}
