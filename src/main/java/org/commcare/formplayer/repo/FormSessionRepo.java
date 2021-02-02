package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FormSessionRepo extends CrudRepository<SerializableFormSession, String> {
    int purge();
    @Query("SELECT *, dateopened::timestamptz as dateopened_timestamp " +
            "FROM %s WHERE username = ? ORDER BY dateopened_timestamp DESC")
    List<SerializableFormSession> findUserSessions(String username);
}
