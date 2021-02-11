package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormSessionRepo extends JpaRepository<SerializableFormSession, String> {
    @Query(
        value = "SELECT * FROM formplayer_sessions WHERE username = :username " +
                "ORDER BY dateopened\\:\\:timestamptz DESC",
        nativeQuery = true
    )
    List<SerializableFormSession> findUserSessions(@Param("username") String username);
}
